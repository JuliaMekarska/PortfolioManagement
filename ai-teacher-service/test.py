
from langchain_huggingface import HuggingFaceEndpoint
from langchain.chains import LLMChain
from langchain_core.prompts import PromptTemplate

# -------- CONFIG --------
HUGGINGFACE_MODEL = "deotech/StocK_Advisor"
HUGGINGFACE_API_TOKEN =

template = """
You are a financial teacher.
Explain clearly and simply the answer to the student's question.
Do not give financial advice to buy/sell, only explain concepts.

Question: {question}
Answer:
"""

prompt = PromptTemplate(input_variables=["question"], template=template)

llm = HuggingFaceEndpoint(
    endpoint_url=f"https://api-inference.huggingface.co/models/{HUGGINGFACE_MODEL}",
    huggingfacehub_api_token=HUGGINGFACE_API_TOKEN,
    temperature= 0.7,
    max_new_tokens= 512
)

chain = LLMChain(prompt=prompt, llm=llm)

# Test
question = "Tell me about ETFs"
response = chain.invoke({"question": question})
print(response)
