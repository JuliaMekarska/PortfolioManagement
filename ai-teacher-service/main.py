# service.py

import os
import uuid
import logging
from typing import Dict, Any, List

from fastapi import FastAPI, APIRouter, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from huggingface_hub import InferenceClient

from langchain.llms.base import LLM
from langchain.chains import ConversationChain
from langchain.memory import ConversationBufferWindowMemory


# ——————————————————————————————————————————————————————————————————————————————
# Logging
# ——————————————————————————————————————————————————————————————————————————————
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("uvicorn.error")


# ——————————————————————————————————————————————————————————————————————————————
# FastAPI + Router
# ——————————————————————————————————————————————————————————————————————————————
app = FastAPI(title="AI Finance Teacher (LangChain wrapper)")
router = APIRouter(prefix="/items", tags=["items"])


# ——————————————————————————————————————————————————————————————————————————————
# HF Token & InferenceClient
# ——————————————————————————————————————————————————————————————————————————————
HF_TOKEN = os.getenv("HF_TOKEN")
if not HF_TOKEN:
    raise RuntimeError("Missing HF_TOKEN environment variable. Set it to your Hugging Face token.")

hf_client = InferenceClient(provider="cohere", api_key=HF_TOKEN)
DEFAULT_MODEL = "CohereLabs/c4ai-command-r-plus"


# ——————————————————————————————————————————————————————————————————————————————
# In‐memory conversation store
# ——————————————————————————————————————————————————————————————————————————————
CONVERSATIONS: Dict[str, ConversationChain] = {}


# ——————————————————————————————————————————————————————————————————————————————
# Pydantic Models
# ——————————————————————————————————————————————————————————————————————————————
class CreateResponse(BaseModel):
    conversation_id: str

class MessageRequest(BaseModel):
    message: str

class MessageResponse(BaseModel):
    answer: str
    conversation_id: str


# ——————————————————————————————————————————————————————————————————————————————
# LangChain LLM wrapper for Cohere via HF InferenceClient
# ——————————————————————————————————————————————————————————————————————————————
class CohereHFChat(LLM):
    # 1) Declare pydantic fields so validation passes
    client: InferenceClient
    model: str = DEFAULT_MODEL
    max_new_tokens: int = 120

    class Config:
        # allow our HF client (non-pydantic type) to sit in a field
        arbitrary_types_allowed = True

    # 2) No need for a custom __init__; pydantic will handle client/model/max_new_tokens for us

    def _call(self, prompt: str, stop: List[str] | None = None) -> str:
        messages = [
            {"role": "system", "content": "You are a helpful finance teacher. Keep answers concise (short/medium)."},
            {"role": "user",   "content": prompt},
        ]
        completion = self.client.chat.completions.create(
            model=self.model,
            messages=messages,
            max_tokens=self.max_new_tokens,
        )
        choices = getattr(completion, "choices", None) or []
        first = choices[0] if choices else completion
        msg = getattr(first, "message", None)
        return str(msg or first)

    @property
    def _identifying_params(self) -> dict:
        # Now this is an attribute, not a method
        return {
            "model": self.model,
            "max_new_tokens": self.max_new_tokens,
        }

    @property
    def _llm_type(self) -> str:
        return "cohere_hf_chat"


# ——————————————————————————————————————————————————————————————————————————————
# /items endpoints
# ——————————————————————————————————————————————————————————————————————————————
@router.get("/hello")
async def hello():
    logger.info("GET /items/hello")
    return {"msg": "hello"}


@router.post("/conversations", response_model=CreateResponse)
def create_conversation(
    k: int = 2,
    model: str = DEFAULT_MODEL,
    max_new_tokens: int = 120
):
    try:
        logger.info("Creating new conversation")
        conv_id = str(uuid.uuid4())

        llm = CohereHFChat(
            client=hf_client,
            model=model,
            max_new_tokens=max_new_tokens
        )
        memory = ConversationBufferWindowMemory(k=k, return_messages=True)
        conv_chain = ConversationChain(llm=llm, memory=memory, verbose=True)

        CONVERSATIONS[conv_id] = conv_chain
        logger.info(f"Stored conversation {conv_id}")

        return {"conversation_id": conv_id}

    except Exception as e:
        logger.error(f"Error in create_conversation: {e}")
        return JSONResponse(status_code=500, content={"error": str(e)})


@router.post("/conversations/{conv_id}/message", response_model=MessageResponse)
def send_message(conv_id: str, body: MessageRequest):
    conv = CONVERSATIONS.get(conv_id)
    if not conv:
        raise HTTPException(status_code=404, detail="Conversation not found")

    answer = conv.predict(input=body.message)
    return {"answer": answer, "conversation_id": conv_id}


@router.get("/conversations/{conv_id}/history")
def get_history(conv_id: str):
    conv = CONVERSATIONS.get(conv_id)
    if not conv:
        raise HTTPException(status_code=404, detail="Conversation not found")

    msgs = [{"role": m.role, "content": m.content} for m in conv.memory.chat_memory.messages]
    return {"history": msgs}


# ——————————————————————————————————————————————————————————————————————————————
# Mount router & uvicorn entrypoint
# ——————————————————————————————————————————————————————————————————————————————
app.include_router(router)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "service:app",
        host="127.0.0.1",
        port=5049,
        reload=True,
    )
