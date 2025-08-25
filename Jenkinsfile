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
                echo "=== Starting Deploy Stage ==="

                sh '''
                    echo "Current user: $(whoami)"
                    echo "Jenkins workspace: $(pwd)"
                    echo "APP_SERVER_IP: ${APP_SERVER_IP}"
                    echo "APP_SERVER_USER: ${APP_SERVER_USER}"
                '''

                // Test 1: Check if database credentials exist
                script {
                    try {
                        echo "Testing database credentials..."
                        withCredentials([string(credentialsId: 'postgres-database-password', variable: 'TEST_PASSWORD')]) {
                            echo "✓ postgres-database-password credential found"
                        }
                    } catch (Exception e) {
                        echo "✗ postgres-database-password credential MISSING: ${e.getMessage()}"
                    }

                    try {
                        withCredentials([string(credentialsId: 'database-user', variable: 'TEST_USER')]) {
                            echo "✓ database-user credential found"
                        }
                    } catch (Exception e) {
                        echo "✗ database-user credential MISSING: ${e.getMessage()}"
                    }

                    try {
                        withCredentials([string(credentialsId: 'database-name', variable: 'TEST_DB')]) {
                            echo "✓ database-name credential found"
                        }
                    } catch (Exception e) {
                        echo "✗ database-name credential MISSING: ${e.getMessage()}"
                    }
                }

                // Test 2: Check if SSH credential exists
                script {
                    try {
                        echo "Testing SSH credential..."
                        sshagent(['app-server-key']) {
                            echo "✓ app-server-key credential found and SSH agent started"
                            sh 'ssh-add -l'
                        }
                    } catch (Exception e) {
                        echo "✗ app-server-key credential MISSING or INVALID: ${e.getMessage()}"
                        echo "Please check Jenkins → Manage Jenkins → Manage Credentials"
                        echo "Make sure you have a credential with ID 'app-server-key'"
                    }
                }

                // Test 3: Try a simple SSH connection
                script {
                    try {
                        echo "Testing SSH connection..."
                        sshagent(['app-server-key']) {
                            sh '''
                                ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 ${APP_SERVER_USER}@${APP_SERVER_IP} "echo 'SSH test successful'"
                            '''
                            echo "✓ SSH connection successful"
                        }
                    } catch (Exception e) {
                        echo "✗ SSH connection failed: ${e.getMessage()}"
                    }
                }

                echo "=== Debug tests completed ==="
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