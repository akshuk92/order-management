pipeline {
    agent any

    tools {
        maven 'Maven3'
        jdk 'JDK17'
    }

    environment {
        APP_NAME        = 'order-management'
        IMAGE_NAME      = "${APP_NAME}:${BUILD_NUMBER}"
        IMAGE_LATEST    = "${APP_NAME}:latest"
        HEALTH_URL = 'http://localhost:8081/actuator/health'
        GIT_REPO_URL = 'https://github.com/akshuk92/order-management.git'
        GIT_BRANCH      = 'main'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    stages {

        stage('Checkout') {
            steps {
                echo "==> Checking out source code from GitHub"
                git branch: "${GIT_BRANCH}", url: "${GIT_REPO_URL}"
            }
        }

        stage('Maven Build') {
            steps {
                echo "==> Building application with Maven"
                sh 'mvn -B clean package -DskipTests'
            }
        }

        stage('Run Unit Tests') {
            steps {
                echo "==> Running unit tests"
                sh 'mvn -B test'
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                echo "==> Building Docker image: ${IMAGE_NAME}"
                sh """
                    docker build -t ${IMAGE_NAME} -t ${IMAGE_LATEST} .
                """
            }
        }

        stage('Docker Compose Deployment') {
            steps {
                echo "==> Deploying application using Docker Compose"
                sh """
                    docker compose down || true
                    docker compose up -d --build
                """
            }
        }

        stage('Health Check') {
            steps {
                echo "==> Verifying application health endpoint"
                script {
                    def maxRetries = 10
                    def retryInterval = 6
                    def healthy = false

                    for (int i = 0; i < maxRetries; i++) {
                        def status = sh(
                            script: "curl -s -o /dev/null -w '%{http_code}' ${HEALTH_URL} || true",
                            returnStdout: true
                        ).trim()

                        if (status == '200') {
                            healthy = true
                            echo "Application is healthy (HTTP ${status})"
                            break
                        }

                        echo "Attempt ${i + 1}/${maxRetries}: application not ready yet (HTTP ${status}). Retrying in ${retryInterval}s..."
                        sleep(retryInterval)
                    }

                    if (!healthy) {
                        error "Health check failed: application did not become healthy at ${HEALTH_URL}"
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline completed successfully. Image: ${IMAGE_NAME}"
        }
        failure {
            echo "Pipeline failed. Check logs above for details."
            sh 'docker compose logs --tail=100 || true'
        }
        always {
            echo "Pipeline finished with status: ${currentBuild.currentResult}"
        }
    }
}
