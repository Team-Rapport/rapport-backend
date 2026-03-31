from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    openai_api_key: str
    frontend_origin: str = "http://localhost:5173"

    class Config:
        env_file = ".env"
        extra = "ignore"
        


settings = Settings()