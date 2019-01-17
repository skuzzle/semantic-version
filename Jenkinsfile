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
            sh 'mvn clean verify -Dmaven.compiler.release=10'
          } catch (err) {
            echo err
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
            sh 'mvn clean verify -Dmaven.compiler.release=11'
          } catch (err) {
            echo err
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
            sh 'mvn clean verify -Dmaven.compiler.release=12'
          } catch (err) {
            echo err
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
            sh 'mvn clean verify -Dmaven.compiler.release=13'
          } catch (err) {
            echo err
          }
        }
      }
    }
  }
}
