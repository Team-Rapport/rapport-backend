from pydantic_settings import BaseSettings
from pathlib import Path

class Settings(BaseSettings):
    openai_api_key: str
    frontend_origin: str = "http://localhost:5173"
    redis_url: str = "redis://redis:6379"
    # Primary Spring URL. Override with SPRING_BASE_URL.
    spring_base_url: str = "http://localhost:8080"
    # Docker-network Spring URL (service name "spring").
    spring_base_url_docker: str = "http://spring:8080"
    internal_service_key: str = "rapport-internal-dev-key"

    class Config:
        env_file = ".env"
        extra = "ignore"

    @property
    def running_in_docker(self) -> bool:
        return Path("/.dockerenv").exists()


settings = Settings()
