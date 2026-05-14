#!/usr/bin/env bash
# ============================================================================
# cleanup.sh - Dọn dẹp repo identity-service-v1
# ============================================================================
# Mục đích:
#   1. Xóa Maven Wrapper THỪA ở từng module con (chỉ giữ 1 cái ở root)
#   2. Xóa .gitignore / .gitattributes THỪA ở từng module con
#   3. Xóa Spring Boot main class SAI VỊ TRÍ trong identity-infrastructure
#   4. Xóa application.yaml LẠC CHỖ trong identity-infrastructure
#   5. Xóa 3 file *ApplicationTests.java mà Spring Initializr tự sinh
#   6. Xóa thư mục package SAI convention: com/personal/identity_xxx
#   7. Tạo thư mục package ĐÚNG convention: com/personal/identity/{core,infrastructure,api}
#
# CÁCH DÙNG:
#   Đặt file này tại ROOT của identity-service-v1 (cùng cấp với pom.xml parent).
#   Chạy:
#     chmod +x cleanup.sh
#     ./cleanup.sh
#
# Sau khi chạy xong:
#   git status   # xem file đã bị xóa
#   git add -A
#   git commit -m "chore: clean up Spring Initializr leftovers, fix package layout"
#   git push
# ============================================================================

set -e  # dừng ngay nếu có lệnh nào fail

# Sanity check: phải có pom.xml parent ở thư mục hiện tại
if [ ! -f "pom.xml" ]; then
    echo "❌ Không thấy pom.xml ở thư mục hiện tại."
    echo "   Hãy 'cd' vào thư mục identity-service-v1 (cùng cấp với pom.xml parent) rồi chạy lại."
    exit 1
fi

# Sanity check: phải có 3 module con
for m in identity-core identity-infrastructure identity-api; do
    if [ ! -d "$m" ]; then
        echo "❌ Không thấy thư mục $m. Bạn có đang ở đúng chỗ không?"
        exit 1
    fi
done

echo "============================================================"
echo "BƯỚC 1/7: Xóa Maven Wrapper thừa trong các module con"
echo "============================================================"
# Maven Wrapper chỉ cần 1 cái ở root. 3 cái ở 3 module con là rác từ Spring Initializr.
for m in identity-core identity-infrastructure identity-api; do
    rm -fv "$m/mvnw"
    rm -fv "$m/mvnw.cmd"
    rm -rfv "$m/.mvn"
done

echo ""
echo "============================================================"
echo "BƯỚC 2/7: Xóa .gitignore / .gitattributes thừa"
echo "============================================================"
# Chỉ cần 1 .gitignore ở root.
for m in identity-core identity-infrastructure identity-api; do
    rm -fv "$m/.gitignore"
    rm -fv "$m/.gitattributes"
done

echo ""
echo "============================================================"
echo "BƯỚC 3/7: Xóa Spring Boot main class SAI VỊ TRÍ trong infrastructure"
echo "============================================================"
# Module infrastructure là ADAPTER LAYER, không phải Spring Boot app.
# Để @SpringBootApplication ở đây sẽ:
#   - Component scan toàn bộ com.personal.identity_infrastructure
#   - Khi build, plugin spring-boot-maven có thể tìm nhầm main class
#   - Tạo thêm 1 fat JAR vô nghĩa khi repackage
rm -fv identity-infrastructure/src/main/java/com/personal/identity_infrastructure/IdentityInfrastructureApplication.java

echo ""
echo "============================================================"
echo "BƯỚC 4/7: Xóa application.yaml lạc chỗ trong infrastructure"
echo "============================================================"
# application.yaml phải nằm ở identity-api/src/main/resources (module chạy app),
# không phải ở infrastructure (chỉ là thư viện adapter, không bootstrap được).
rm -fv identity-infrastructure/src/main/resources/application.yaml

echo ""
echo "============================================================"
echo "BƯỚC 5/7: Xóa 3 file *ApplicationTests.java của Spring Initializr"
echo "============================================================"
# 3 file này dùng @SpringBootTest nhưng không có main class trong cùng module
# (trừ api sau khi mình thêm) → sẽ fail khi chạy. Ta sẽ viết test thật sau.
rm -fv identity-core/src/test/java/com/personal/identity_core/IdentityCoreApplicationTests.java
rm -fv identity-infrastructure/src/test/java/com/personal/identity_infrastructure/IdentityInfrastructureApplicationTests.java
rm -fv identity-api/src/test/java/com/personal/identity_api/IdentityApiApplicationTests.java

echo ""
echo "============================================================"
echo "BƯỚC 6/7: Xóa thư mục package SAI convention (identity_xxx)"
echo "============================================================"
# Java convention: package KHÔNG dùng underscore.
# Sai: com.personal.identity_core
# Đúng: com.personal.identity.core
# Bây giờ các thư mục này đã rỗng (đã xóa file ở bước 3, 5), xóa luôn thư mục.
rm -rfv identity-core/src/test/java/com/personal/identity_core
rm -rfv identity-infrastructure/src/test/java/com/personal/identity_infrastructure
rm -rfv identity-infrastructure/src/main/java/com/personal/identity_infrastructure
rm -rfv identity-api/src/test/java/com/personal/identity_api

echo ""
echo "============================================================"
echo "BƯỚC 7/7: Tạo cây thư mục package ĐÚNG convention"
echo "============================================================"
# Base package: com.personal.identity
# Module sub-package:
#   identity-core           → com.personal.identity.core
#   identity-infrastructure → com.personal.identity.infrastructure
#   identity-api            → com.personal.identity.api
mkdir -pv identity-core/src/main/java/com/personal/identity/core
mkdir -pv identity-core/src/test/java/com/personal/identity/core
mkdir -pv identity-infrastructure/src/main/java/com/personal/identity/infrastructure
mkdir -pv identity-infrastructure/src/test/java/com/personal/identity/infrastructure
mkdir -pv identity-api/src/main/java/com/personal/identity/api
mkdir -pv identity-api/src/test/java/com/personal/identity/api
mkdir -pv identity-api/src/main/resources/db/migration

# Maven mặc định bỏ qua thư mục rỗng khi build. Để Git theo dõi cây thư mục mới,
# tạm thêm 1 file .gitkeep trong mỗi thư mục lá. File này sẽ tự bị xóa khi
# chúng ta bắt đầu thêm code Java thật vào.
touch identity-core/src/main/java/com/personal/identity/core/.gitkeep
touch identity-core/src/test/java/com/personal/identity/core/.gitkeep
touch identity-infrastructure/src/main/java/com/personal/identity/infrastructure/.gitkeep
touch identity-infrastructure/src/test/java/com/personal/identity/infrastructure/.gitkeep
touch identity-api/src/main/java/com/personal/identity/api/.gitkeep
touch identity-api/src/test/java/com/personal/identity/api/.gitkeep
touch identity-api/src/main/resources/db/migration/.gitkeep

echo ""
echo "============================================================"
echo "✅ HOÀN TẤT. Cây thư mục mới:"
echo "============================================================"
# Hiển thị cấu trúc kết quả để xác nhận
if command -v tree >/dev/null 2>&1; then
    tree -L 6 -I 'target|node_modules' --dirsfirst
else
    find . -type d -not -path '*/target/*' -not -path '*/.git/*' -not -path '*/.idea/*' | sort
fi

echo ""
echo "============================================================"
echo "BƯỚC TIẾP THEO:"
echo "============================================================"
echo "  git status                                                "
echo "  git add -A                                                "
echo "  git commit -m \"chore: clean up Spring Initializr leftovers, fix package layout\""
echo "  git push                                                  "
echo "============================================================"
