"""One-shot: Keycloak password grant (feeder) + POST /api/v1/odds-feed via api-gateway."""
import json
import subprocess
import sys

TOKEN_URL = "http://localhost:8090/realms/sportsbetting/protocol/openid-connect/token"
FEED_URL = "http://localhost:8070/api/v1/odds-feed"
BODY = json.dumps(
    [{"eventId": "evt-agent-smoke-1", "selection": "HOME", "odds": 1.95}]
)


def main() -> int:
    tr = subprocess.run(
        [
            "curl",
            "-s",
            "-X",
            "POST",
            TOKEN_URL,
            "-H",
            "Content-Type: application/x-www-form-urlencoded",
            "-d",
            "grant_type=password&client_id=odds-gateway&username=feeder&password=feeder",
        ],
        capture_output=True,
        text=True,
        check=False,
    )
    if tr.returncode != 0:
        print(tr.stderr, file=sys.stderr)
        return tr.returncode
    try:
        token = json.loads(tr.stdout)["access_token"]
    except (json.JSONDecodeError, KeyError) as e:
        print(tr.stdout, file=sys.stderr)
        print(e, file=sys.stderr)
        return 1

    fr = subprocess.run(
        [
            "curl",
            "-s",
            "-i",
            "-X",
            "POST",
            FEED_URL,
            "-H",
            "Content-Type: application/json",
            "-H",
            f"Authorization: Bearer {token}",
            "-d",
            BODY,
        ],
        capture_output=True,
        text=True,
        check=False,
    )
    sys.stdout.write(fr.stdout)
    if fr.stderr:
        sys.stderr.write(fr.stderr)
    return fr.returncode


if __name__ == "__main__":
    raise SystemExit(main())
