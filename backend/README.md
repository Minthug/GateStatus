GateStatus 프로젝트 포트폴리오
📋 프로젝트 개요
GateStatus는 대한민국 정치인들의 활동을 실시간으로 추적하고 분석하는 정치 정보 플랫폼입니다. 국회 공개 API와 AI 기술을 활용하여 정치인의 발언, 투표, 법안 발의 등을 종합 분석하고 시각화합니다.
🎯 프로젝트 목표

정치인의 활동을 투명하게 공개하여 정치적 책임성 강화
AI 기반 팩트체크 시스템으로 정보의 신뢰성 확보
복잡한 정치 정보를 일반 국민이 쉽게 이해할 수 있도록 시각화


🛠 기술 스택
Backend

Framework: Spring Boot 3.2.3 (Java 17)
Database: PostgreSQL (메인), MongoDB (발언 데이터)
Cache: Redis (세션, 캐시)
Message Queue: RabbitMQ, Apache Kafka
API: Spring WebFlux (비동기 처리)
Security: Spring Security
AI: OpenAI GPT-4 API

Infrastructure & DevOps

Container: Docker, Kubernetes
CI/CD: Google Jib
Monitoring: Spring Actuator
External API: 국회 공개 API, 네이버 뉴스 API


🏗 시스템 아키텍처
도메인 구조
GateStatus/
├── domain/
│   ├── figure/          # 정치인 관리
│   ├── statement/       # 발언 관리 (MongoDB)
│   ├── vote/            # 투표 정보
│   ├── proposedBill/    # 법안 발의
│   ├── issue/           # 이슈 관리
│   ├── comparison/      # 정치인 비교 분석
│   ├── dashboard/       # 대시보드 통계
│   ├── category/        # 카테고리 분류
│   ├── tag/             # 태그 시스템
│   └── timeline/        # 타임라인 이벤트
└── global/
├── config/          # 설정 (Security, Redis, Kafka 등)
├── openAi/          # AI 기능
└── kubernetes/      # 쿠버네티스 설정
멀티 데이터베이스 전략

PostgreSQL: 정형 데이터 (정치인, 투표, 법안)
MongoDB: 비정형 데이터 (발언, 텍스트 분석)
Redis: 캐싱 및 세션 관리