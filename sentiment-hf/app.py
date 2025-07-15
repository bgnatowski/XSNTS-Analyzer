# app.py
from enum import Enum
from fastapi import FastAPI
from pydantic import BaseModel
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import torch

MODEL = "tabularisai/multilingual-sentiment-analysis"
tokenizer = AutoTokenizer.from_pretrained(MODEL)
model     = AutoModelForSequenceClassification.from_pretrained(MODEL)
model.eval()

sentiment_map = {0: "NEG", 1: "NEG", 2: "NEU", 3: "POS", 4: "POS"}

class Request(BaseModel):
    text: str

class Label(str, Enum):
    POSITIVE = "POS"
    NEGATIVE = "NEG"
    NEUTRAL  = "NEU"

class Response(BaseModel):
    sentiment: Label
    score: float   # prawdopodobieństwo klasy w 3-stopniowej skali

app = FastAPI()

@app.post("/sentiment", response_model=Response)
def classify(req: Request):
    inputs = tokenizer(req.text,
                       return_tensors="pt",
                       truncation=True,
                       padding=True,
                       max_length=512)
    with torch.no_grad():
        logits = model(**inputs).logits
    probs = torch.softmax(logits, dim=-1)[0]
    five_cls = int(torch.argmax(probs))
    tri_cls  = sentiment_map[five_cls]        # POS / NEU / NEG
    score    = probs[five_cls].item()
    return {"sentiment": tri_cls, "score": round(score, 4)}
