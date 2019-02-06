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
        buildWithJdk("10")
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
        buildWithJdk("11")
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
        buildWithJdk("12")
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
        buildWithJdk("13")
      }
    }
  }
}

void buildWithJdk(version) {
  stage("Show Versions") {
    script {
        sh 'mvn -version'
        sh 'java -version'
        sh 'javac -version'
    }
  }
  
  stage("Clean Maven Project") {
    script {
      sh 'mvn clean -Dmaven.clean.failOnError=false -Dmaven.clean.retryOnError=true'
    }
  }
  
  stage("Build with JDK $version") {
    script {
      try {
        sh "mvn integration-test -Dmaven.compiler.release=$version"
      } catch (err) {
        currentBuild.result = 'FAILURE'
      }
    }
  }
}