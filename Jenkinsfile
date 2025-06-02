pipeline {
  environment {
    deplRegistry = 'ghcr.io/datakaveri/cat-prod'
    registryUri = 'https://ghcr.io'
    registryCredential = 'datakaveri-ghcr'
    GIT_HASH = GIT_COMMIT.take(7)
  }
  agent { 
    node {
      label 'slave1' 
    }
  }
  stages {

    stage('Building images') {
      steps{
        script {
          echo 'Pulled - ' + env.GIT_BRANCH
          deplImage = docker.build( deplRegistry, "-f ./docker/prod.dockerfile .")
        }
      }
    }
    stage('Continuous Deployment') {
      when {
        allOf {
          anyOf {
            changeset "docker/**"
            changeset "docs/**"
            changeset "pom.xml"
            changeset "src/main/**"
            triggeredBy cause: 'UserIdCause'
          }
          expression {
            return env.GIT_BRANCH == 'origin/tgdex';
          }
        }
      }
      stages {
        stage('Push Images') {
          steps {
            script {
              docker.withRegistry( registryUri, registryCredential ) {
                deplImage.push("tgdex-5.6.0-${env.GIT_HASH}")
              }
            }
          }
        }
        stage('Docker Swarm deployment') {
          steps {
            script {
              sh "ssh azureuser@docker-swarm 'docker service update cat-tgdex_cat-tgdex --image ghcr.io/datakaveri/cat-prod:tgdex-5.6.0-${env.GIT_HASH}'"
              sh 'sleep 10'
            }
          }
          post{
            failure{
              error "Failed to deploy image in Docker Swarm"
            }
          }
        }
      }
    }
  }
  post{
    failure{
      script{
        if (env.GIT_BRANCH == 'origin/tgdex')
        emailext recipientProviders: [buildUser(), developers()], to: '$RS_RECIPIENTS, $DEFAULT_RECIPIENTS', subject: '$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!', body: '''$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS:
Check console output at $BUILD_URL to view the results.'''
      }
    }
  }
}
