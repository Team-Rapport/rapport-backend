from fastapi import APIRouter, HTTPException
from app.model.schemas import (
    SessionCreateRequest, ChatRequest, ChatResponse,
    FinalizeRequest,
)
from app.service import chat_service
import uuid
import logging

logger = logging.getLogger("uvicorn.error")

router = APIRouter(prefix="/ai", tags=["chat"])


@router.post("/session")
async def create_session(req: SessionCreateRequest):
    """챗봇 세션을 생성한다. consent=True 필수."""
    if not req.consent:
        raise HTTPException(
            status_code=400,
            detail="AI 사전 점검에 동의해야 세션을 시작할 수 있습니다.",
        )
    session_id = str(uuid.uuid4())
    initial_message = await chat_service.create_session(session_id)
    return {"session_id": session_id, "initial_message": initial_message}


@router.post("/message", response_model=ChatResponse)
async def send_message(req: ChatRequest):
    """사용자 메시지를 전송하고 AI 응답을 받는다."""
    try:
        result = await chat_service.chat(req.session_id, req.message)
        return ChatResponse(
            session_id=req.session_id,
            reply=result["reply"],
            turn=result["turn"],
            max_turn=result["max_turn"],
            is_final=result["is_final"],
            is_crisis=result["is_crisis"],
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/finalize")
async def finalize_session(req: FinalizeRequest):
    """세션을 종료하고 리포트를 생성한 뒤 Spring에 저장한다."""
    try:
        result = await chat_service.finalize(req.session_id, req.spring_session_id, req.user_id)
        return result
    except ValueError as e:
        logger.warning(
            "Finalize failed (client error): session_id=%s, spring_session_id=%s, user_id=%s, error=%s",
            req.session_id, req.spring_session_id, req.user_id, str(e)
        )
        raise HTTPException(status_code=400, detail=str(e))
    except RuntimeError as e:
        logger.error(
            "Finalize failed (server dependency error): session_id=%s, spring_session_id=%s, user_id=%s, error=%s",
            req.session_id, req.spring_session_id, req.user_id, str(e)
        )
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        logger.exception(
            "Finalize failed (unexpected): session_id=%s, spring_session_id=%s, user_id=%s",
            req.session_id, req.spring_session_id, req.user_id
        )
        raise HTTPException(status_code=500, detail=str(e))
