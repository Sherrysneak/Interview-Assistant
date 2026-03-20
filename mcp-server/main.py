"""Interview Guide MCP server.

采用 FastMCP 作为核心协议实现，并保留 /tools/invoke 兼容路由，
避免牵动 Java 侧既有调用链路。
"""

from __future__ import annotations

import inspect
import os
from collections.abc import Awaitable, Callable
from typing import Any

import httpx
from fastmcp import FastMCP
from pydantic import BaseModel, Field
from starlette.requests import Request
from starlette.responses import JSONResponse

INTERNAL_TOKEN = os.getenv("MCP_INTERNAL_TOKEN", "dev-internal-token")
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN", "")

mcp = FastMCP(
    name="Interview Resource MCP",
    instructions=(
        "Provide learning resources for interview preparation. "
        "Tools include repository search, docs search, practice problems and articles."
    ),
)


class ToolInvokeRequest(BaseModel):
    """兼容模式工具调用请求。"""

    tool: str
    params: dict[str, Any] = Field(default_factory=dict)


class ToolInvokeResponse(BaseModel):
    """兼容模式工具调用响应。"""

    data: list[dict[str, Any]] = Field(default_factory=list)


async def _search_repositories(query: str) -> list[dict[str, Any]]:
    """调用 GitHub Search API 搜索仓库。"""

    headers = {"Accept": "application/vnd.github+json"}
    if GITHUB_TOKEN:
        headers["Authorization"] = f"Bearer {GITHUB_TOKEN}"

    params = {
        "q": f"{query} in:name,description readme",
        "sort": "stars",
        "order": "desc",
        "per_page": 5,
    }

    try:
        async with httpx.AsyncClient(timeout=4.0) as client:
            resp = await client.get(
                "https://api.github.com/search/repositories",
                params=params,
                headers=headers,
            )
            resp.raise_for_status()
            items = resp.json().get("items", [])
            return [
                {
                    "title": item.get("full_name", "unknown"),
                    "url": item.get("html_url", ""),
                    "platform": "GitHub",
                    "reason": f"stars: {item.get('stargazers_count', 0)}",
                }
                for item in items
                if item.get("html_url")
            ]
    except Exception:
        return [
            {
                "title": "spring-projects/spring-boot",
                "url": "https://github.com/spring-projects/spring-boot",
                "platform": "GitHub",
                "reason": "兜底推荐",
            }
        ]


def _mock_docs(query: str) -> list[dict[str, Any]]:
    """返回文档资源兜底数据。"""

    return [
        {
            "title": f"Spring 官方文档 - {query}",
            "url": "https://docs.spring.io/spring-boot/reference/",
            "platform": "Spring Docs",
            "reason": "官方文档优先",
        },
        {
            "title": f"Redis 官方文档 - {query}",
            "url": "https://redis.io/docs/latest/",
            "platform": "Redis Docs",
            "reason": "补齐基础与进阶能力",
        },
    ]


def _mock_problems(query: str) -> list[dict[str, Any]]:
    """返回题库资源兜底数据。"""

    return [
        {
            "title": f"LeetCode 热题 - {query}",
            "url": "https://leetcode.com/problemset/",
            "platform": "LeetCode",
            "reason": "面试高频题",
        },
        {
            "title": f"牛客专项练习 - {query}",
            "url": "https://www.nowcoder.com/exam/intelligent",
            "platform": "NowCoder",
            "reason": "笔试实战",
        },
    ]


def _mock_articles(query: str) -> list[dict[str, Any]]:
    """返回文章资源兜底数据。"""

    return [
        {
            "title": f"掘金实战文章 - {query}",
            "url": "https://juejin.cn/",
            "platform": "掘金",
            "reason": "案例导向",
        },
        {
            "title": f"InfoQ 主题文章 - {query}",
            "url": "https://www.infoq.cn/",
            "platform": "InfoQ",
            "reason": "工程实践",
        },
    ]


@mcp.tool
async def repo_search(query: str = "java backend") -> list[dict[str, Any]]:
    """搜索开源仓库资源。"""

    return await _search_repositories(query)


@mcp.tool
def doc_search(query: str) -> list[dict[str, Any]]:
    """搜索技术文档资源。"""

    return _mock_docs(query)


@mcp.tool
def problem_search(query: str) -> list[dict[str, Any]]:
    """搜索练习题资源。"""

    return _mock_problems(query)


@mcp.tool
def article_search(query: str) -> list[dict[str, Any]]:
    """搜索文章资源。"""

    return _mock_articles(query)


async def _handle_repo_search(query: str) -> list[dict[str, Any]]:
    """兼容层仓库检索处理。"""

    return await _search_repositories(query)


def _handle_doc_search(query: str) -> list[dict[str, Any]]:
    """兼容层文档检索处理。"""

    return _mock_docs(query)


def _handle_problem_search(query: str) -> list[dict[str, Any]]:
    """兼容层题目检索处理。"""

    return _mock_problems(query)


def _handle_article_search(query: str) -> list[dict[str, Any]]:
    """兼容层文章检索处理。"""

    return _mock_articles(query)


ToolHandler = Callable[[str], list[dict[str, Any]] | Awaitable[list[dict[str, Any]]]]


TOOL_HANDLERS: dict[str, ToolHandler] = {
    "repo-search": _handle_repo_search,
    "doc-search": _handle_doc_search,
    "problem-search": _handle_problem_search,
    "article-search": _handle_article_search,
}


@mcp.custom_route("/health", methods=["GET"])
async def health_route(_: Request) -> JSONResponse:
    """健康检查接口。"""

    return JSONResponse({"status": "UP"})


@mcp.custom_route("/tools/invoke", methods=["POST"])
async def invoke_tool_route(request: Request) -> JSONResponse:
    """兼容旧调用协议的工具路由。

    Java 端仍可通过 /tools/invoke + X-Internal-Token 进行调用。
    """

    if request.headers.get("X-Internal-Token", "") != INTERNAL_TOKEN:
        return JSONResponse({"detail": "invalid token"}, status_code=401)

    try:
        body = await request.json()
        req = ToolInvokeRequest.model_validate(body)
    except Exception:
        return JSONResponse({"detail": "invalid request"}, status_code=400)

    handler = TOOL_HANDLERS.get(req.tool)
    if handler is None:
        return JSONResponse(ToolInvokeResponse(data=[]).model_dump())

    query = str(req.params.get("query", "")).strip()
    if req.tool == "repo-search" and not query:
        query = "java backend"

    result = handler(query)
    if inspect.isawaitable(result):
        result = await result

    payload = ToolInvokeResponse(data=result)
    return JSONResponse(payload.model_dump())


app = mcp.http_app()


if __name__ == "__main__":
    mcp.run(transport="http", host="0.0.0.0", port=8086)
