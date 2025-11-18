# SchoolNet 백엔드 배포 가이드

## 목차
1. [새로 추가된 기능](#새로-추가된-기능)
2. [환경 설정](#환경-설정)
3. [로컬 테스트 방법](#로컬-테스트-방법)
4. [EC2 배포 방법](#ec2-배포-방법)
5. [API 테스트 방법](#api-테스트-방법)
6. [문제 해결](#문제-해결)

---

## 새로 추가된 기능

### 1. 고급 AI 콘텐츠 필터링
문제점: "야이 멍청아" 같은 변형 욕설이 기존 필터를 통과함

해결책:
- Gemini API를 활용한 10가지 기준 욕설 탐지
  1. 명시적 욕설 (시발, 개새끼, 병신 등)
  2. 자음 욕설 (ㅅㅂ, ㄲㅈ, ㅂㅅ 등)
  3. 띄어쓰기 우회 (시 발, 개 새 끼 등)
  4. 특수문자 우회 (시.발, 개*새*끼 등)
  5. 변형 욕설 (멍청아, 바보야, 찌질이, 루저 등)
  6. 따돌림성 표현
  7. 은어/비하 표현
  8. 성적 암시
  9. 개인정보 노출
  10. 비방/명예훼손

구현 파일:
- `ContentFilterService.java` - 핵심 필터링 로직
- `FilterResult.java` - 필터링 결과 DTO
- `FilterLog.java` - 필터링 로그 엔티티

### 2. 신고 시스템
기능:
- 사용자가 게시글/댓글 신고 가능
- 중복 신고 방지 (같은 사용자가 같은 콘텐츠 여러 번 신고 불가)
- 신고 5건 누적 시 자동 블라인드 처리

구현 파일:
- `Report.java` - 신고 엔티티
- `ReportService.java` - 신고 로직 (자동 블라인드 포함)
- `ReportController.java` - 신고 API 엔드포인트

### 3. 관리자 시스템
기능:
- 관리자가 신고 내역 조회 (대기/승인/거부 상태별)
- 신고 승인 시 콘텐츠 삭제 및 작성자 제재
- 신고 거부 시 콘텐츠 복구
- 사용자 제재 관리 (경고/일시정지/영구정지)

구현 파일:
- `AdminService.java` - 관리자 기능 로직
- `AdminController.java` - 관리자 API 엔드포인트
- `UserPenalty.java` - 사용자 제재 엔티티

### 4. 선배 인증 시스템
기능:
- 2~3학년 학생이 학생증으로 선배 인증
- 질문 게시판은 인증된 선배만 답변 가능

구현 파일:
- `UserController.java` - 선배 인증 엔드포인트 추가
- `CommentService.java` - 질문 게시판 댓글 작성 시 선배 여부 확인

---

## 환경 설정

### 1. Java 버전 확인
```bash
java -version
# Java 17 이상 필요 (Spring Boot 3.5.6 요구사항)
```

**Java 17이 없는 경우**:
```bash
# Conda 사용 시
conda install -c conda-forge openjdk=17 -y

# 또는 apt 사용 시 (Ubuntu)
sudo apt update
sudo apt install openjdk-17-jdk -y
```

### 2. application.properties 설정
```bash
cd src/main/resources
cp application.properties.template application.properties
```

필수 수정 항목:
```properties
# 1. Gemini API 키 (https://makersuite.google.com/app/apikey)
gemini.api.key=여기에_실제_API_키_입력

# 2. OCR Space API 키 (https://ocr.space/ocrapi)
ocr.space.api.key=여기에_실제_API_키_입력

# 3. JWT Secret Key (최소 32자 이상)
jwt.secret=여기에_강력한_시크릿_키_입력

# 4. MySQL 설정 (EC2 배포 시)
spring.datasource.url=jdbc:mysql://localhost:3306/schoolnet
spring.datasource.username=실제_MySQL_사용자명
spring.datasource.password=실제_MySQL_비밀번호
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

### 3. MySQL 설치 및 설정 (EC2용)
```bash
# MySQL 설치
sudo apt install mysql-server -y

# MySQL 시작
sudo systemctl start mysql
sudo systemctl enable mysql

# 데이터베이스 생성
sudo mysql
```

```sql
CREATE DATABASE schoolnet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'schoolnet_user'@'localhost' IDENTIFIED BY '강력한비밀번호';
GRANT ALL PRIVILEGES ON schoolnet.* TO 'schoolnet_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

---

## 로컬 테스트 방법

### 1. H2 메모리 DB로 빠른 테스트
```bash
cd /path/to/cloudproject_backend

# application.properties에서 H2 설정 활성화
# (기본적으로 template에 H2 설정이 되어 있음)

# Maven 빌드 및 실행
./mvnw clean package
./mvnw spring-boot:run
```

**H2 Console 접속**: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:schoolnet`
- Username: `sa`
- Password: (비워두기)

### 2. Swagger로 API 테스트
**Swagger UI 접속**: http://localhost:8080/swagger-ui/index.html

---

## EC2 배포 방법

### 준비 사항
- AWS Academy Lab Start 실행
- EC2 인스턴스 Public IP 확인
- 보안 그룹에서 포트 8080 오픈

### 배포 단계

#### 1. GitHub에서 최신 코드 Pull
```bash
ssh -i your-key.pem ec2-user@your-ec2-ip

cd /path/to/cloudproject_backend
git pull origin main
```

#### 2. application.properties 설정
```bash
cd src/main/resources
cp application.properties.template application.properties
nano application.properties

# 위 "환경 설정" 섹션대로 API 키와 MySQL 설정 입력
```

#### 3. Maven 빌드
```bash
cd /path/to/cloudproject_backend
./mvnw clean package -DskipTests
```

#### 4. 백그라운드 실행
```bash
# 기존 프로세스 종료
lsof -ti:8080 | xargs kill -9

# 새로 실행 (nohup으로 백그라운드 실행)
nohup ./mvnw spring-boot:run > app.log 2>&1 &

# 로그 확인
tail -f app.log
```

#### 5. 배포 확인
```bash
# 애플리케이션 실행 확인
curl http://localhost:8080/api/health

# 외부에서 접속 테스트
curl http://YOUR_EC2_PUBLIC_IP:8080/api/health
```

---

## API 테스트 방법

### 1. 회원가입 및 로그인
```bash
# 회원가입
curl -X POST http://YOUR_EC2_IP:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@schoolnet.com",
    "password": "test1234",
    "username": "테스트유저",
    "schoolName": "테스트중학교"
  }'

# 로그인
curl -X POST http://YOUR_EC2_IP:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@schoolnet.com",
    "password": "test1234"
  }'

# 응답에서 JWT 토큰 복사
```

### 2. 욕설 필터링 테스트
```bash
# JWT 토큰을 환경변수로 설정
export JWT_TOKEN="eyJhbGc..."

# 게시글 작성 (욕설 포함)
curl -X POST http://YOUR_EC2_IP:8080/api/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "title": "테스트 제목",
    "content": "야이 멍청아 이거 테스트야",
    "boardId": 1
  }'

# 기대 결과: 400 Bad Request
# "변형 욕설이 감지되었습니다."
```

### 3. 신고 시스템 테스트
```bash
# 게시글 신고
curl -X POST http://YOUR_EC2_IP:8080/api/reports \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "targetType": "POST",
    "targetId": 1,
    "reason": "PROFANITY",
    "detail": "욕설이 포함되어 있습니다"
  }'

# 같은 사용자가 5번 신고하면 (다른 사용자로 로그인해서)
# 5번째 신고 시 자동으로 게시글 블라인드 처리됨
```

### 4. 선배 인증 테스트
```bash
# 선배 인증 (2학년)
curl -X POST http://YOUR_EC2_IP:8080/api/users/senior-verification \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "grade": 2,
    "studentIdImageUrl": "https://example.com/student-id.jpg"
  }'

# 질문 게시판에 댓글 작성 (인증된 선배만 가능)
curl -X POST http://YOUR_EC2_IP:8080/api/comments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "postId": 1,
    "content": "이 문제는 이렇게 풀면 돼"
  }'
```

---

## 문제 해결

### 1. "Java version mismatch" 오류
```bash
# Java 17 설치 확인
java -version

# Java 17이 없으면 설치
conda install -c conda-forge openjdk=17 -y
```

### 2. 포트 충돌 오류
```bash
# 포트 사용 프로세스 확인
lsof -ti:8080

# 프로세스 강제 종료
lsof -ti:8080 | xargs kill -9

# 또는 application.properties에서 포트 변경
server.port=8082
```

### 3. MySQL 연결 오류
```bash
# MySQL 실행 확인
sudo systemctl status mysql

# 안 되면 재시작
sudo systemctl restart mysql

# 연결 테스트
mysql -u schoolnet_user -p
```

### 4. API 키 오류 (Gemini/OCR)
```bash
# application.properties 확인
cat src/main/resources/application.properties | grep api.key

# 올바른 API 키가 설정되어 있는지 확인
# Gemini API: https://makersuite.google.com/app/apikey
# OCR Space: https://ocr.space/ocrapi
```

### 5. 403 Forbidden 오류
- `SecurityConfig.java`에서 엔드포인트 권한 확인
- JWT 토큰이 올바른지 확인
- 로그에서 Spring Security 로그 확인

---

## 추가 지원

문제가 발생하면:
1. `app.log` 파일 확인
2. GitHub Issues에 로그와 함께 문제 등록
3. 팀원들에게 카톡으로 문의

---

## 테스트 체크리스트

SchoolNet 백엔드 배포 후 다음 항목들을 테스트하세요:

- [ ] 회원가입/로그인 동작 확인
- [ ] 욕설 필터링 동작 확인 ("야이 멍청아" 테스트)
- [ ] 신고 기능 동작 확인
- [ ] 5건 신고 시 자동 블라인드 확인
- [ ] 관리자 신고 검토 확인
- [ ] 선배 인증 동작 확인
- [ ] 질문 게시판 선배만 답변 가능 확인
