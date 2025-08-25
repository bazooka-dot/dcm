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

               // Test if we can run basic commands
               sh '''
                   echo "Current user: $(whoami)"
                   echo "Jenkins workspace: $(pwd)"
                   echo "Environment variables:"
                   echo "APP_SERVER_IP: ${APP_SERVER_IP}"
                   echo "APP_SERVER_USER: ${APP_SERVER_USER}"
               '''

               // Test SSH credential exists and works
               sshagent(['app-server-key']) {
                   sh '''
                       echo "✓ SSH agent started"
                       echo "SSH keys loaded:"
                       ssh-add -l || echo "No keys found in SSH agent"

                       echo "Testing SSH connection..."
                       ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -v ${APP_SERVER_USER}@${APP_SERVER_IP} "
                           echo 'SSH connection successful!'
                           whoami
                           pwd
                           docker --version || echo 'Docker not found'
                           docker compose version || echo 'Docker Compose not found'
                       "
                   '''
               }

               echo "=== SSH test completed ==="
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