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
                          maven:3.9-openjdk-21 \
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
                          maven:3.9-openjdk-21 \
                          mvn checkstyle:check spotbugs:check || true
                    '''
                }
            }
        }

        stage('Test') {
            steps {
                dir('DCMapplication') {
                    sh '''
                        docker run --rm \
                          -v "$PWD":/usr/src/app \
                          -w /usr/src/app \
                          maven:3.9-openjdk-21 \
                          mvn clean test
                    '''
                }
                publishTestResults testResultsPattern: 'DCMapplication/target/surefire-reports/*.xml'
            }
        }

        stage('Build') {
            steps {
                dir('DCMapplication') {
                    sh '''
                        docker run --rm \
                          -v "$PWD":/usr/src/app \
                          -w /usr/src/app \
                          maven:3.9-openjdk-21 \
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
                    usernamePassword(credentialsId: 'postgres-db', usernameVariable: 'POSTGRES_USER', passwordVariable: 'POSTGRES_PASSWORD')
                ]) {
                    sshagent(['app-server-ssh-key']) {
                        sh '''
                            # Copy application files to home directory
                            ssh -o StrictHostKeyChecking=no ${APP_SERVER_USER}@${APP_SERVER_IP} "mkdir -p ~/dcm"
                            scp -r DCMapplication/* ${APP_SERVER_USER}@${APP_SERVER_IP}:~/dcm/

                            # Deploy with docker compose
                            ssh ${APP_SERVER_USER}@${APP_SERVER_IP} "
                                cd ~/dcm
                                export POSTGRES_DB=dcm
                                export POSTGRES_USER='${POSTGRES_USER}'
                                export POSTGRES_PASSWORD='${POSTGRES_PASSWORD}'
                                docker compose down || true
                                docker compose up -d --build
                            "
                        '''
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