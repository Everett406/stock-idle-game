#!/usr/bin/env bash
# 一次性配置发布签名密钥到 GitHub Secrets：
#   1. 生成 RSA 2048 密钥库（若不存在）
#   2. Base64 编码密钥库 → SIGNING_KEY
#   3. 写入 KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD
#
# 用法：
#   pip install pynacl
#   export GH_TOKEN=你的GitHubToken          # 只放环境变量，严禁写入文件
#   ./scripts/setup-signing-secrets.sh <GitHub用户名> <仓库名> [别名] [密码]
#
# 密钥库与密码说明：
#   - 默认密码随机生成，并保存在仓库外的 ~/.stockidle-signing/ 下
#   - 密钥库本身通过 SIGNING_KEY（Base64）存放在 GitHub Secrets 中，仓库不留任何密钥材料
set -euo pipefail

OWNER="${1:?用法: setup-signing-secrets.sh <用户名> <仓库> [别名] [密码]}"
REPO="${2:?用法: setup-signing-secrets.sh <用户名> <仓库> [别名] [密码]}"
ALIAS="${3:-stockidle}"
KEY_DIR="$HOME/.stockidle-signing"
KS="$KEY_DIR/release.keystore"

mkdir -p "$KEY_DIR"
if [ ! -f "$KS" ]; then
  PASS="${4:-$(openssl rand -hex 16)}"
  echo "==> 生成密钥库 $KS (alias=$ALIAS)"
  keytool -genkeypair -v -keystore "$KS" -alias "$ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$PASS" -keypass "$PASS" \
    -dname "CN=StockIdle, O=StockIdle, C=CN"
  echo "$PASS" > "$KEY_DIR/password.txt"
  chmod 600 "$KEY_DIR/password.txt" "$KS"
else
  PASS="$(cat "$KEY_DIR/password.txt")"
  echo "==> 复用已存在的密钥库 $KS"
fi

command -v python3 >/dev/null || { echo "需要 python3"; exit 1; }
python3 -c "import nacl" 2>/dev/null || pip3 install pynacl

set_secret() {
  local name="$1" value="$2"
  echo "==> 写入 Secret: $name"
  python3 - "$name" "$value" "$OWNER/$REPO" <<'PY'
import base64, json, os, sys
import urllib.request
from nacl import encoding, public

name, value, repo = sys.argv[1], sys.argv[2], sys.argv[3]
token = os.environ["GH_TOKEN"]
hdr = {
    "Authorization": f"Bearer {token}",
    "Accept": "application/vnd.github+json",
    "User-Agent": "stock-idle-setup",
}
req = urllib.request.Request(f"https://api.github.com/repos/{repo}/actions/secrets/public-key", headers=hdr)
with urllib.request.urlopen(req) as r:
    pk = json.load(r)
key = public.PublicKey(pk["key"].encode(), encoding.Base64Encoder())
sealed = public.SealedBox(key).encrypt(value.encode())
body = json.dumps({"encrypted_value": base64.b64encode(sealed).decode(), "key_id": pk["key_id"]}).encode()
req = urllib.request.Request(
    f"https://api.github.com/repos/{repo}/actions/secrets/{name}",
    data=body, headers={**hdr, "Content-Type": "application/json"}, method="PUT",
)
with urllib.request.urlopen(req) as r:
    print(f"    {name}: HTTP {r.status}")
PY
}

B64="$(base64 -w0 "$KS")"
set_secret "SIGNING_KEY" "$B64"
set_secret "KEYSTORE_PASSWORD" "$PASS"
set_secret "KEY_ALIAS" "$ALIAS"
set_secret "KEY_PASSWORD" "$PASS"

echo ""
echo "✅ 4 个 Secrets 已配置完成：SIGNING_KEY / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD"
echo "   密钥库备份位置（仓库外）：$KS"
echo "   密码备份位置（仓库外）：  $KEY_DIR/password.txt"
