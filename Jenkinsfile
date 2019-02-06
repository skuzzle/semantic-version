pipeline {
  agent none
  stages {
    stage('JDK-10') {
      agent {
        docker {
          image 'maven:3.6-jdk-10'
          args '-v $HOME/.m2:/root/.m2 -u 0:0'
        }
      }
      steps {
        script {
          try {
            sh 'mvn -version'
            sh 'java -version'
            sh 'javac -version'
            sh 'mvn clean integration-test -Dmaven.compiler.release=10'
          } catch (err) {
            currentBuild.result = 'FAILURE'
          }
        }
      }
    }
    stage('JDK-11') {
      agent {
        docker {
          image 'maven:3.6-jdk-11'
          args '-v $HOME/.m2:/root/.m2 -u 0:0'
        }
      }
      steps {
        script {
          try {
            sh 'mvn -version'
            sh 'java -version'
            sh 'javac -version'
            sh 'mvn clean integration-test -Dmaven.compiler.release=11'
          } catch (err) {
            currentBuild.result = 'FAILURE'
          }
        }
      }
    }
    stage('JDK-12') {
      agent {
        docker {
          image 'maven:3.6-jdk-12'
          args '-v $HOME/.m2:/root/.m2 -u 0:0'
        }
      }
      steps {
        script {
          try {
            sh 'mvn -version'
            sh 'java -version'
            sh 'javac -version'
            sh 'mvn clean integration-test -Dmaven.compiler.release=12'
          } catch (err) {
            currentBuild.result = 'FAILURE'
          }
        }
      }
    }
    stage('JDK-13') {
      agent {
        docker {
          image 'maven:3.6-jdk-13'
          args '-v $HOME/.m2:/root/.m2 -u 0:0'
        }
      }
      steps {
        script {
          try {
            sh 'mvn -version'
            sh 'java -version'
            sh 'javac -version'
            sh 'mvn clean integration-test -Dmaven.compiler.release=13'
          } catch (err) {
            currentBuild.result = 'FAILURE'
          }
        }
      }
    }
  }
}
