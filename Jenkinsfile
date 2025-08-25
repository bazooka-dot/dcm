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
        stage('Deploy to App Server') {
            steps {
                withCredentials([
                    string(credentialsId: 'postgres-database-password', variable: 'POSTGRES_PASSWORD'),
                    string(credentialsId: 'database-user', variable: 'POSTGRES_USER'),
                    string(credentialsId: 'database-name', variable: 'POSTGRES_DB')
                ]) {
                    sshagent(['app-server-key']) {
                        script {
                            try {
                                // Test SSH connectivity first
                                sh '''
                                    echo "Testing SSH connectivity..."
                                    ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 ${APP_SERVER_USER}@${APP_SERVER_IP} "echo 'SSH connection successful'"
                                '''

                                // Copy application files with verbose output
                                sh '''
                                    echo "Creating directory on remote server..."
                                    ssh -o StrictHostKeyChecking=no ${APP_SERVER_USER}@${APP_SERVER_IP} "mkdir -p ~/dcm"

                                    echo "Copying files to remote server..."
                                    scp -v -r DCMapplication/* ${APP_SERVER_USER}@${APP_SERVER_IP}:~/dcm/
                                '''

                                // Deploy with detailed logging
                                sh '''
                                    echo "Starting deployment..."
                                    ssh -o StrictHostKeyChecking=no ${APP_SERVER_USER}@${APP_SERVER_IP} "
                                        set -e  # Exit on any error
                                        cd ~/dcm

                                        echo 'Current directory contents:'
                                        ls -la

                                        echo 'Setting environment variables...'
                                        export POSTGRES_DB='${POSTGRES_DB}'
                                        export POSTGRES_USER='${POSTGRES_USER}'
                                        export POSTGRES_PASSWORD='${POSTGRES_PASSWORD}'

                                        echo 'Environment variables set:'
                                        echo 'POSTGRES_DB=' \$POSTGRES_DB
                                        echo 'POSTGRES_USER=' \$POSTGRES_USER
                                        echo 'POSTGRES_PASSWORD is set'

                                        echo 'Stopping existing containers...'
                                        docker compose down --remove-orphans -v 2>&1 || echo 'No containers to stop'

                                        echo 'Checking docker-compose.yml exists...'
                                        if [ ! -f docker-compose.yml ]; then
                                            echo 'ERROR: docker-compose.yml not found!'
                                            ls -la
                                            exit 1
                                        fi

                                        echo 'Starting new containers...'
                                        docker compose up -d --build --force-recreate

                                        echo 'Checking container status...'
                                        docker compose ps
                                        docker compose logs --tail=50
                                    "
                                '''
                            } catch (Exception e) {
                                echo "Deployment failed with error: ${e.getMessage()}"
                                // Get remote logs for debugging
                                sh '''
                                    echo "Attempting to get remote system info for debugging..."
                                    ssh -o StrictHostKeyChecking=no ${APP_SERVER_USER}@${APP_SERVER_IP} "
                                        echo 'System info:'
                                        uname -a
                                        echo 'Docker version:'
                                        docker --version || echo 'Docker not found'
                                        echo 'Docker Compose version:'
                                        docker compose version || echo 'Docker Compose not found'
                                        echo 'Available space:'
                                        df -h
                                        echo 'Memory usage:'
                                        free -h
                                    " || echo "Could not retrieve remote system info"
                                '''
                                throw e
                            }
                        }
                    }
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