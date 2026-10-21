pipeline {
    agent any
    tools {
        jdk 'jdk8'
        maven 'maven3'
    }
    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    stages {
        stage('构建与部署') {
            steps {
                withCredentials([file(credentialsId: 'cy-admin-prod-env', variable: 'DEPLOY_ENV_FILE')]) {
                    dir('server') { sh 'sh deploy/jenkins-build.sh' }
                }
            }
        }
    }
}
