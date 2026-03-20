# MCP Server (FastMCP)

This service is implemented with FastMCP as the core protocol server.

It exposes:

- Native MCP HTTP endpoint at `/mcp`
- Compatibility route for backend integration at `/tools/invoke`
- Health endpoint at `/health`

## Local development

1. Create and activate a virtual environment:

```bash
python3 -m venv .venv
source .venv/bin/activate
```

2. Install dependencies:

```bash
pip install -r requirements.txt
```

3. Run the server:

```bash
python main.py
```

4. Health check:

```bash
curl http://localhost:8086/health
```

## Why editor may show unresolved imports

If `fastmcp`, `httpx`, or `pydantic` is marked as unresolved in the editor, your Python interpreter is usually not pointing to the local `.venv`. Select the interpreter from `.venv` and the warnings should disappear.

## Docker run

This project supports Docker Compose. The `mcp-server` container installs dependencies from `requirements.txt` and starts via `python main.py`.
