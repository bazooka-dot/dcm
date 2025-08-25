pipeline {
    agent any

    environment {
        APP_SERVER_IP = '74.242.217.71'
        APP_SERVER_USER = 'dcm'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo "Checked out code from GitHub"
            }
        }

        stage('Security Scan - Dependencies') {
            steps {
                dir('DCMapplication') {
                    sh '''
                        docker run --rm \
                          -v "$PWD":/usr/src/app \
                          -w /usr/src/app \
                          maven:3.9.6-eclipse-temurin-21-alpine \
                          mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7 || true
                    '''
                }
                publishHTML([
                    allowMissing: true,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'DCMapplication/target',
                    reportFiles: 'dependency-check-report.html',
                    reportName: 'OWASP Dependency Check'
                ])
            }
        }

        stage('Static Code Analysis') {
            steps {
                dir('DCMapplication') {
                    sh '''
                        docker run --rm \
                          -v "$PWD":/usr/src/app \
                          -w /usr/src/app \
                          maven:3.9.6-eclipse-temurin-21-alpine \
                          mvn checkstyle:check spotbugs:check || true
                    '''
                }
            }
        }

        stage('Build') {
            steps {
                dir('DCMapplication') {
                    sh '''
                        docker run --rm \
                          -v "$PWD":/usr/src/app \
                          -w /usr/src/app \
                          maven:3.9.6-eclipse-temurin-21-alpine \
                          mvn clean package -DskipTests
                    '''
                }
                archiveArtifacts artifacts: 'DCMapplication/target/*.jar'
            }
        }

        stage('Container Security Scan') {
            steps {
                dir('DCMapplication') {
                    sh '''
                        docker build -t dcm-app:${BUILD_NUMBER} .
                        docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                          aquasec/trivy:latest image dcm-app:${BUILD_NUMBER} || true
                    '''
                }
            }
        }
        stage('Deploy') {
            steps {
                sshagent(['app-server-key']) {
                    sh '''
                        scp -o StrictHostKeyChecking=no DCMapplication/target/*.jar ${APP_SERVER_USER}@${APP_SERVER_IP}:~/dcm/DCMaaplication/target/
                    '''
                    sh '''
                        ssh -o StrictHostKeyChecking=no ${APP_SERVER_USER}@${APP_SERVER_IP} "
                            cd ~/dcm/DCMaaplication
                            docker compose down
                            docker compose up -d --build
                        "
                    '''
                }
            }
        }
        stage('Health Check') {
            steps {
                script {
                    sleep(60)
                    sh '''
                        timeout 300 bash -c 'until curl -f http://${APP_SERVER_IP}:8080/devices; do sleep 10; done'
                        echo "Application is healthy!"
                    '''
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo 'Deployment completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}