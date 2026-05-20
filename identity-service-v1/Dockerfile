# =============================================================================
# Dockerfile — multi-stage build cho identity-service
# =============================================================================
# Stage 1 (builder): JDK 21 đầy đủ + Maven → build fat JAR
# Stage 2 (runtime): JRE 21 slim + non-root user → chạy app
#
# Vì sao multi-stage:
#   - Image cuối chỉ ~200MB thay vì ~600MB (loại bỏ JDK, Maven cache, source code).
#   - Source code KHÔNG nằm trong image production → giảm bề mặt tấn công, không
#     lộ business logic nếu image bị leak.
#   - Build cache theo layer: lần build sau, nếu pom.xml không đổi, Maven dependency
#     đã download không tải lại.
#
# Build:
#   podman build -t identity-service:latest .
#   (Cũng chạy được với docker, BuildKit tự bật cho Dockerfile syntax >=1.4)
#
# Run (cần env var như application-prod.yml yêu cầu):
#   podman run -d --name identity-app \
#     -e JWT_SECRET=$(openssl rand -base64 48) \
#     -e SPRING_DATASOURCE_URL=jdbc:oracle:thin:@host.containers.internal:1521/FREEPDB1 \
#     -e SPRING_DATASOURCE_USERNAME=identity \
#     -e SPRING_DATASOURCE_PASSWORD=identity123 \
#     -e SPRING_DATA_REDIS_HOST=host.containers.internal \
#     -e SPRING_PROFILES_ACTIVE=prod \
#     -e GEOIP_FAIL_ON_MISSING=false \
#     -p 8080:8080 \
#     identity-service:latest
# =============================================================================

# syntax=docker/dockerfile:1.7
# (cho phép --mount=type=cache, RUN heredoc, v.v.)

# -----------------------------------------------------------------------------
# Stage 1: builder
# -----------------------------------------------------------------------------
# eclipse-temurin = OpenJDK build của Adoptium, phổ biến và miễn phí cho commercial.
# Dùng 21-jdk-jammy (Ubuntu 22.04 base) thay vì alpine vì:
#   - musl libc của alpine đôi khi có vấn đề với Oracle JDBC driver.
#   - jammy có glibc, tương thích tốt hơn với native libraries.
#
# LƯU Ý: dùng FQIN (Fully Qualified Image Name) "docker.io/library/..." thay vì
# "eclipse-temurin:..." trần. Lý do:
#   - Podman (đặc biệt rootless Fedora/Arch) bật short-name resolution mode.
#     Image tên ngắn → Podman không tự đoán registry, đòi user chọn tương tác.
#     Build trong CI / non-TTY sẽ fail với lỗi
#     "short-name resolution enforced but cannot prompt without a TTY".
#   - Docker tự gắn docker.io ngầm — FQIN không gây bất tiện gì cho docker.
#   - Vậy nên dùng FQIN là an toàn cho mọi engine.
FROM docker.io/library/eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Cài Maven version cố định (3.9.x) thay vì dùng mvn wrapper trong repo,
# vì wrapper cần download lúc build → tốn thời gian + phụ thuộc network.
# Maven 3.9.x tương thích Spring Boot 3.5.x + Java 21.
ARG MAVEN_VERSION=3.9.9
ARG MAVEN_SHA512=a555254d6b53d267965a3404ecb14e53c3827c09c3b94b5678835887ab404556bfaf78dcfe03ba76fa2508649dca8531c74bca4d5846513522404d48e8c4ac8b

RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends curl ca-certificates; \
    rm -rf /var/lib/apt/lists/*; \
    curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
        -o /tmp/maven.tar.gz; \
    echo "${MAVEN_SHA512}  /tmp/maven.tar.gz" | sha512sum -c -; \
    tar -xzf /tmp/maven.tar.gz -C /opt; \
    ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn; \
    rm /tmp/maven.tar.gz

# -- Layer 1: pom.xml --
# Copy CHỈ pom.xml trước → cache dependency download. Code thay đổi không invalid
# layer này, build sau cực nhanh.
COPY pom.xml ./
COPY identity-core/pom.xml ./identity-core/
COPY identity-infrastructure/pom.xml ./identity-infrastructure/
COPY identity-api/pom.xml ./identity-api/

# Download dependency offline-mode. -B = batch (no interactive log).
# Dùng BuildKit cache mount để cache ~/.m2/repository qua các lần build.
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B dependency:go-offline -DskipTests || true
# `|| true` vì go-offline với multi-module thường fail nhẹ (cố tải dependency
# của module chưa build), nhưng đa số đã cached → bước build chính sẽ ổn.

# -- Layer 2: source code --
COPY identity-core/src ./identity-core/src
COPY identity-infrastructure/src ./identity-infrastructure/src
COPY identity-api/src ./identity-api/src

# Build fat JAR. Skip test vì:
#   - CI đã chạy test ở workflow riêng (xem .github/workflows/ci.yml).
#   - Test cần Oracle + Redis, không nên dồn vào docker build.
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B clean package -DskipTests -pl identity-api -am

# Verify JAR tồn tại + đổi tên thành /build/app.jar để stage runtime copy gọn.
# Lưu ý: pom.xml có <finalName>identity-api</finalName> → tên file là
# "identity-api.jar" (KHÔNG có version suffix). Đừng dùng glob "identity-api-*.jar"
# vì không match — sẽ fail ngay step này.
# spring-boot-maven-plugin còn sinh file "identity-api.jar.original" (JAR gốc trước
# khi repackage fat) — ta KHÔNG cần file đó, chỉ lấy fat JAR.
RUN set -eux; \
    cd identity-api/target; \
    ls -lh; \
    test -f identity-api.jar; \
    cp identity-api.jar /build/app.jar

# -----------------------------------------------------------------------------
# Stage 2: runtime
# -----------------------------------------------------------------------------
# JRE only (không cần javac, jar tool, ...). 21-jre-jammy: ~200MB.
FROM docker.io/library/eclipse-temurin:21-jre-jammy AS runtime

# Metadata (OCI labels — chuẩn cho image registry)
LABEL org.opencontainers.image.title="identity-service" \
      org.opencontainers.image.description="Identity microservice — JWT auth, session management, RBAC" \
      org.opencontainers.image.source="https://github.com/vut94201-rgb/hanyanghanyang" \
      org.opencontainers.image.licenses="MIT"

# -- Non-root user --
# Vì sao: chạy root trong container = nếu attacker exploit RCE thì có root trên
# container, vẫn bị giới hạn bởi namespace nhưng vẫn risk. Best practice cho mọi
# image production là chạy với uid không phải 0.
ARG APP_UID=10001
ARG APP_GID=10001
RUN groupadd --system --gid ${APP_GID} app && \
    useradd --system --uid ${APP_UID} --gid app --home-dir /app --shell /usr/sbin/nologin app && \
    mkdir -p /app /var/lib/identity/geoip && \
    chown -R app:app /app /var/lib/identity

# Cài tini làm PID 1 → handle signal đúng (Ctrl+C / podman stop sẽ gửi SIGTERM
# trực tiếp xuống Java thay vì process tree bị treo).
# Cài curl cho healthcheck (busybox không có sẵn trong jre-jammy).
RUN apt-get update && \
    apt-get install -y --no-install-recommends tini curl ca-certificates && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy JAR từ stage builder. --chown để user app sở hữu trực tiếp, không cần chmod sau.
COPY --from=builder --chown=app:app /build/app.jar /app/app.jar

# JVM flags production:
#   -XX:+UseContainerSupport (default từ JDK 10+, ghi cho rõ): JVM đọc đúng
#       memory limit của container (cgroup) thay vì memory host.
#   -XX:MaxRAMPercentage=75: dùng tối đa 75% RAM container cho heap.
#   -XX:+ExitOnOutOfMemoryError: OOM thì exit luôn để orchestrator restart,
#       không cố gắng tiếp tục với heap hỏng.
#   -Djava.security.egd=file:/dev/./urandom: tránh treo lúc start (SecureRandom
#       đọc /dev/random có thể block nếu entropy thấp, vấn đề kinh điển trong container).
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

# Đặt sẵn profile prod làm default cho image. Override khi cần:
#   podman run -e SPRING_PROFILES_ACTIVE=dev ...
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

USER app

# Healthcheck dùng endpoint /api/health (đã verified public + light).
# --interval 30s: check mỗi 30 giây sau khi container start.
# --start-period 60s: cho app 60s warmup (Oracle connect + Flyway + JPA init).
# --retries 3: 3 lần fail liên tiếp mới đánh dấu unhealthy.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/api/health || exit 1

# tini làm PID 1, exec java làm con. Dùng exec form (JSON array) chứ KHÔNG shell
# form — để signal propagate đúng. $JAVA_OPTS expand qua sh -c vì JSON form
# không expand env var.
ENTRYPOINT ["/usr/bin/tini", "--"]
CMD ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
