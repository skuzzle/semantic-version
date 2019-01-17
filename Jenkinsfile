pipeline {
  agent {
    docker {
      image 'openjdk:11-jdk-slim'
      args '-v $HOME/.m2:/root/.m2 -u 0:0'
    }
  }
  stages {
    stage('Build') {
      steps {
        sh 'mvn clean verify'
      }
    }
  }
}
