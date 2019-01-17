pipeline {
  stages {
    stage('Build') {
      parallel {
        stage('JDK-13') {
          agent {
            docker {
              image 'maven:3.6-jdk-13'
              args '-v $HOME/.m2:/root/.m2 -u 0:0'
            }
          }
          steps {
            sh 'mvn clean verify -Dmaven.compiler.release=13'
          }
        }
      }
    }
  }
}
