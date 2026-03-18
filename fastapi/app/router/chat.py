from fastapi import APIRouter, HTTPException
from app.model.schemas import (
    SessionCreateRequest, ChatRequest, ChatResponse,
    FinalizeRequest, ReportResponse
)
from app.service import chat_service
import uuid

router = APIRouter(prefix="/ai", tags=["chat"])

@router.post("/session")
async def create_session(req: SessionCreateRequest):
    session_id = str(uuid.uuid4())
    await chat_service.create_session(session_id)
    return {"session_id": session_id}

@router.post("/message", response_model=ChatResponse)
async def send_message(req: ChatRequest):
    try:
        reply = await chat_service.chat(req.session_id, req.message)
        return ChatResponse(session_id=req.session_id, reply=reply)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/finalize")
async def finalize_session(req: FinalizeRequest):
    result = await chat_service.finalize(req.session_id)
    return result