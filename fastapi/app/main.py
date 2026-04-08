from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.router import chat
from app.core.config import settings

app = FastAPI(title="Rapport AI Server")

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
    settings.frontend_origin,
    "http://localhost:8080",
    "http://127.0.0.1:5500",
    "http://localhost:5500",
    ],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chat.router)

@app.get("/ai/health")
async def health():
    return {"status": "ok"}