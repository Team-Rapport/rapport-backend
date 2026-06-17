from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.router import chat
from app.core.config import settings
import logging

app = FastAPI(title="Rapport AI Server")
logger = logging.getLogger("uvicorn.error")

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

@app.on_event("startup")
async def log_startup_config():
    logger.info(
        "FastAPI startup config: FRONTEND_ORIGIN=%s, SPRING_BASE_URL=%s, SPRING_BASE_URL_DOCKER=%s, RUNNING_IN_DOCKER=%s",
        settings.frontend_origin,
        settings.spring_base_url,
        settings.spring_base_url_docker,
        settings.running_in_docker,
    )

@app.get("/ai/health")
async def health():
    return {"status": "ok"}
