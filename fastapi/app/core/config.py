from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    openai_api_key: str
    frontend_origin: str = "http://localhost:5173"
    redis_url: str = "redis://redis:6379"
    spring_base_url: str = "http://spring:8080"
    internal_service_key: str = "dev-internal-secret"

    class Config:
        env_file = ".env"
        extra = "ignore"


settings = Settings()