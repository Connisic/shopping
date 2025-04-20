# 澶氶樁娈垫瀯寤?- 绗竴闃舵锛氭瀯寤洪樁娈?
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# 璁剧疆宸ヤ綔鐩綍
WORKDIR /build

# 棣栧厛澶嶅埗鍏ㄩ儴鐨勬簮浠ｇ爜鍒版瀯寤虹洰褰?
COPY . /build/

# 涓嶄娇鐢╣o-offline锛岀洿鎺ヤ娇鐢ㄩ拡瀵瑰崟涓ā鍧楃殑鏋勫缓鍛戒护
# 杩欐牱閬垮厤Maven灏濊瘯瑙ｆ瀽鎵€鏈夋ā鍧?
RUN mvn clean package -DskipTests -pl shopping_category_service -am

# 绗簩闃舵锛氳繍琛岄樁娈?- 浣跨敤鏇村皬鐨勫熀纭€闀滃儚
FROM eclipse-temurin:17-jre-alpine

# 璁剧疆宸ヤ綔鐩綍
WORKDIR /app

# 娣诲姞鏍囩
LABEL maintainer="Shopping Team"
LABEL description="Category Service for Shopping Platform"

# 鍒涘缓闈瀝oot鐢ㄦ埛
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 璁剧疆JVM杩愯鍙傛暟锛岄檺鍒跺唴瀛樹娇鐢?
ENV JAVA_OPTS="-server -Xms96m -Xmx192m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"

# 璁剧疆鏃跺尯
ENV TZ=Asia/Shanghai

# 瀹夎鍩烘湰宸ュ叿锛堝彲閫夛紝濡傞渶鏇村皬闀滃儚鍙互绉婚櫎锛?
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    apk del tzdata

# 浠庢瀯寤洪樁娈靛鍒禞AR鍖?
COPY --from=builder /build/shopping_category_service/target/*.jar /app/app.jar

# 鍒囨崲鍒伴潪root鐢ㄦ埛
USER appuser

# 鏆撮湶鏈嶅姟绔彛
EXPOSE 9004

# 鍋ュ悍妫€鏌?
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:9004/actuator/health || exit 1

# 璁剧疆鍏ュ彛鐐?
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
