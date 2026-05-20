#!/usr/bin/env bash
# =============================================================================
# download-geolite2.sh — tải MaxMind GeoLite2-City database
# =============================================================================
# Cách dùng:
#   1) Đăng ký account miễn phí tại https://www.maxmind.com/en/geolite2/signup
#   2) Vào "Manage License Keys" → tạo license key mới
#   3) Set env var:
#        export MAXMIND_LICENSE_KEY=xxxxxxxxxx
#   4) Chạy:
#        ./scripts/download-geolite2.sh
#
# Script tải file .tar.gz, extract .mmdb ra docker/geoip/GeoLite2-City.mmdb
# (path khớp với GEOIP_DATABASE_PATH default trong application.yml dev profile).
#
# Vì sao cần script này:
#   - MaxMind từ 2019 yêu cầu license key (kể cả tier free), không tải
#     trực tiếp được nữa.
#   - File ~70MB, đổi 2 lần/tuần. Production nên cron script này hàng tuần
#     để giữ DB cập nhật (GeoIP nội bộ MaxMind cũng update theo nhịp đó).
#   - .mmdb format binary — KHÔNG commit vào git (đã thêm vào .gitignore).
# =============================================================================

set -euo pipefail

# ---- Cấu hình ---------------------------------------------------------------

EDITION_ID="${EDITION_ID:-GeoLite2-City}"
OUTPUT_DIR="${OUTPUT_DIR:-./docker/geoip}"
TMP_DIR="$(mktemp -d)"

# Cleanup tmp dir kể cả khi script fail
trap 'rm -rf "$TMP_DIR"' EXIT

# ---- Validate ---------------------------------------------------------------

if [[ -z "${MAXMIND_LICENSE_KEY:-}" ]]; then
    echo "ERROR: MAXMIND_LICENSE_KEY không được set." >&2
    echo "       Đăng ký tại https://www.maxmind.com/en/geolite2/signup" >&2
    echo "       rồi: export MAXMIND_LICENSE_KEY=<key của bạn>" >&2
    exit 1
fi

# Kiểm tra dependency
for cmd in curl tar sha256sum; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
        echo "ERROR: thiếu lệnh '$cmd'. Cài đặt rồi chạy lại." >&2
        exit 1
    fi
done

# ---- Download ---------------------------------------------------------------

BASE_URL="https://download.maxmind.com/app/geoip_download"
DOWNLOAD_URL="${BASE_URL}?edition_id=${EDITION_ID}&license_key=${MAXMIND_LICENSE_KEY}&suffix=tar.gz"
CHECKSUM_URL="${BASE_URL}?edition_id=${EDITION_ID}&license_key=${MAXMIND_LICENSE_KEY}&suffix=tar.gz.sha256"

TARBALL="${TMP_DIR}/${EDITION_ID}.tar.gz"
CHECKSUM_FILE="${TMP_DIR}/${EDITION_ID}.tar.gz.sha256"

echo "==> Đang tải ${EDITION_ID}.tar.gz..."
curl -fsSL --retry 3 --retry-delay 5 -o "$TARBALL" "$DOWNLOAD_URL"

echo "==> Đang tải checksum..."
curl -fsSL --retry 3 --retry-delay 5 -o "$CHECKSUM_FILE" "$CHECKSUM_URL"

# ---- Verify checksum --------------------------------------------------------
# MaxMind trả về định dạng: "<sha256>  <filename>" — sha256sum đọc được trực tiếp
# nếu tên file khớp. Ta copy tarball vào cùng thư mục với checksum để verify.

echo "==> Verify SHA-256..."
(
    cd "$TMP_DIR"
    sha256sum -c "$(basename "$CHECKSUM_FILE")" --status \
        || { echo "ERROR: checksum không khớp — file có thể bị corrupt hoặc bị MITM." >&2; exit 1; }
)
echo "    ✓ Checksum OK"

# ---- Extract ----------------------------------------------------------------

echo "==> Extract..."
tar -xzf "$TARBALL" -C "$TMP_DIR"

# Tarball của MaxMind có cấu trúc: GeoLite2-City_YYYYMMDD/GeoLite2-City.mmdb
MMDB_FILE="$(find "$TMP_DIR" -name "${EDITION_ID}.mmdb" -type f | head -1)"

if [[ -z "$MMDB_FILE" ]]; then
    echo "ERROR: không tìm thấy ${EDITION_ID}.mmdb trong tarball." >&2
    exit 1
fi

# ---- Install ----------------------------------------------------------------

mkdir -p "$OUTPUT_DIR"
DEST="${OUTPUT_DIR}/${EDITION_ID}.mmdb"

# Atomic move: copy ra tmp file cùng filesystem rồi rename — nếu app đang đọc
# file cũ, không bị half-written file.
cp "$MMDB_FILE" "${DEST}.tmp"
mv "${DEST}.tmp" "$DEST"

SIZE_MB=$(( $(stat -c%s "$DEST" 2>/dev/null || stat -f%z "$DEST") / 1024 / 1024 ))

echo ""
echo "==> Hoàn tất."
echo "    File:  $DEST"
echo "    Size:  ${SIZE_MB} MB"
echo ""
echo "Để app đọc, set:"
echo "  export GEOIP_DATABASE_PATH=$(realpath "$DEST")"
echo ""
echo "Hoặc giữ default trong application.yml (path tương đối ./docker/geoip/...)."
