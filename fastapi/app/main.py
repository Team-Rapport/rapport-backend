from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.router import chat
from app.core.config import settings

app = FastAPI(title="Rapport AI Server")

app.add_middleware(
    CORSMiddleware,
    allow_origins=[settings.frontend_origin, "http://localhost:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chat.router)

@app.get("/ai/health")
async def health():
    return {"status": "ok"}