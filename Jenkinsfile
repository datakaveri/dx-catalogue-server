pipeline {
  environment {
    devRegistry = 'ghcr.io/datakaveri/cat-dev'
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
    stage('Conditional Execution') {
      when {
        anyOf {
          triggeredBy 'UserIdCause'
          expression {
            def comment = env.ghprbCommentBody?.trim()
            return comment && comment.toLowerCase() != "null"
          }
          changeset "docker/**"
          changeset "docs/**"
          changeset "pom.xml"
          changeset "src/main/**"
        }
      }
      stages {
        stage('Trivy Code Scan (Dependencies)') {
          steps {
            script {
              sh '''
                trivy fs --scanners vuln,secret,misconfig --output trivy-fs-report.txt .
              '''
            }
          }
        }
        stage('Building images') {
          steps{
            script {
              echo 'Pulled - ' + env.GIT_BRANCH
              devImage = docker.build( devRegistry, "-f ./docker/dev.dockerfile .")
              deplImage = docker.build( deplRegistry, "-f ./docker/prod.dockerfile .")
            }
          }
        }
        stage('Trivy Docker Image Scan and Report') {
          steps {
            script {
              sh "trivy image --output trivy-dev-image-report.txt ${devImage.imageName()}"
              sh "trivy image --output trivy-depl-image-report.txt ${deplImage.imageName()}"
            }
          }
          post {
            always {
              archiveArtifacts artifacts: 'trivy-*.txt', allowEmptyArchive: true
              publishHTML(target: [
                allowMissing: true,
                keepAll: true,
                reportDir: '.',
                reportFiles: 'trivy-fs-report.txt, trivy-dev-image-report.txt, trivy-depl-image-report.txt',
                reportName: 'Trivy Reports'
              ])
            }
          }
        }

        stage('Unit Tests and CodeCoverage Test'){
          steps{
            script{
              sh 'sudo update-alternatives --set java /usr/lib/jvm/java-21-openjdk-amd64/bin/java'
              sh 'cp /home/ubuntu/configs/cat-config-test.json ./configs/config-test.json'
              sh 'mvn clean test checkstyle:checkstyle pmd:pmd'
            }
            xunit (
              thresholds: [ skipped(failureThreshold: '75'), failed(failureThreshold: '0') ],
              tools: [ JUnit(pattern: 'target/surefire-reports/*.xml') ]
            )
            jacoco classPattern: 'target/classes', execPattern: 'target/*.exec', sourcePattern: 'src/main/java', exclusionPattern: 'iudx/catalogue/server/apiserver/*,iudx/catalogue/server/deploy/*,iudx/catalogue/server/mockauthenticator/*,iudx/catalogue/server/**/*EBProxy.*,iudx/catalogue/server/**/*ProxyHandler.*,iudx/catalogue/server/**/reactivex/*,**/Constants.class,**/*Verticle.class,iudx/catalogue/server/auditing/util/Constants.class,iudx/catalogue/server/database/DatabaseService.class,iudx/catalogue/server/database/postgres/PostgresService.class'
          }
          post{
            always{
              recordIssues(
                enabledForFailure: true,
                skipBlames: true,
                qualityGates: [[threshold:40, type: 'TOTAL', unstable: false]],
                tool: checkStyle(pattern: 'target/checkstyle-result.xml')
              )
              recordIssues(
                enabledForFailure: true,
                skipBlames: true,
                qualityGates: [[threshold:11, type: 'TOTAL', unstable: false]],
                tool: pmdParser(pattern: 'target/pmd.xml')
              )
            }
            failure{
              error "Test failure. Stopping pipeline execution!"
            }
            cleanup{
              script{
                sh 'sudo update-alternatives --set java /usr/lib/jvm/java-11-openjdk-amd64/bin/java'
                sh 'sudo rm -rf target/'
              }
            }
          }
        }

        stage('Start Cat-Server for Performance and Integration Testing'){
          steps{
            script{
                sh 'scp Jmeter/CatalogueServer.jmx jenkins@jenkins-master:/var/lib/jenkins/iudx/cat/Jmeter/'
                sh 'docker compose up -d perfTest'
                sh 'sleep 45'
            }
          }
          post{
            failure{
              script{
                sh 'docker compose down --remove-orphans'
              }
            }
          }
        }

        stage('Run Jmeter Performance Tests'){
          steps{
            node('built-in') {
              script{
                sh 'rm -rf /var/lib/jenkins/iudx/cat/Jmeter/Report ; mkdir -p /var/lib/jenkins/iudx/cat/Jmeter/Report ; /var/lib/jenkins/apache-jmeter-5.4.1/bin/jmeter.sh -n -t /var/lib/jenkins/iudx/cat/Jmeter/CatalogueServer.jmx -l /var/lib/jenkins/iudx/cat/Jmeter/Report/JmeterTest.jtl -e -o /var/lib/jenkins/iudx/cat/Jmeter/Report'
              }
              perfReport filterRegex: '', showTrendGraphs: true, sourceDataFiles: '/var/lib/jenkins/iudx/cat/Jmeter/Report/*.jtl'
            }
          }
          post{
            failure{
              script{
                sh 'docker compose down --remove-orphans'
              }
              error "Test failure. Stopping pipeline execution!"
            }
          }
        }

        stage('Integration Tests and OWASP ZAP pen test'){
          steps{
            node('built-in') {
              script{
                startZap ([host: '0.0.0.0', port: 8090, zapHome: '/var/lib/jenkins/tools/com.cloudbees.jenkins.plugins.customtools.CustomTool/OWASP_ZAP/ZAP_2.11.0'])
                sh 'curl http://0.0.0.0:8090/JSON/pscan/action/disableScanners/?ids=10096'
              }
            }
            script{
                sh 'sudo update-alternatives --set java /usr/lib/jvm/java-21-openjdk-amd64/bin/java'
                sh 'scp /home/ubuntu/configs/cat-config-test.json ./configs/config-test.json'
                sh 'mvn test-compile failsafe:integration-test -DskipUnitTests=true -DintTestProxyHost=jenkins-master-priv -DintTestProxyPort=8090 -DintTestHost=jenkins-slave1 -DintTestPort=8080'
            }
            node('built-in') {
              script{
                runZapAttack()
              }
            }
          }
          post{
            always{
              xunit (
                thresholds: [ skipped(failureThreshold: '0'), failed(failureThreshold: '0') ],
                tools: [ JUnit(pattern: 'target/failsafe-reports/*.xml') ]
                )
              node('built-in') {
                script{
                  archiveZap failHighAlerts: 1, failMediumAlerts: 1, failLowAlerts: 1
                }
              }
            }
            failure{
              error "Test failure. Stopping pipeline execution!"
            }
            cleanup{
              script{
                sh 'sudo update-alternatives --set java /usr/lib/jvm/java-11-openjdk-amd64/bin/java'
                sh 'docker compose down --remove-orphans'
              } 
            }
          }
        }

        stage('Continuous Deployment') {
          when {
              expression { return env.GIT_BRANCH == 'origin/master'; }
          }
          stages {
            stage('Push Images') {
              steps {
                script {
                  docker.withRegistry( registryUri, registryCredential ) {
                    devImage.push("6.0.0-alpha-${env.GIT_HASH}")
                    deplImage.push("6.0.0-alpha-${env.GIT_HASH}")
                  }
                }
              }
            }
            stage('Docker Swarm deployment') {
              steps {
                script {
                  sh "ssh azureuser@docker-swarm 'docker service update cat_cat --image ghcr.io/datakaveri/cat-prod:6.0.0-alpha-${env.GIT_HASH}'"
                  sh 'sleep 10'
                }
              }
              post{
                failure{
                  error "Failed to deploy image in Docker Swarm"
                }
              }
            }
            stage('Integration test on swarm deployment') {
              steps {
                script{
                  sh 'sudo update-alternatives --set java /usr/lib/jvm/java-21-openjdk-amd64/bin/java'
                  sh 'mvn test-compile failsafe:integration-test -DskipUnitTests=true -DintTestDepl=true'
                }
              }
              post{
                always{
                  xunit (
                    thresholds: [ skipped(failureThreshold: '0'), failed(failureThreshold: '0') ],
                    tools: [ JUnit(pattern: 'target/failsafe-reports/*.xml') ]
                  )
                }
                failure{
                  error "Test failure. Stopping pipeline execution!"
                }
                cleanup{
                  script{
                    sh 'sudo update-alternatives --set java /usr/lib/jvm/java-11-openjdk-amd64/bin/java'
                  }
                }
              }
            }
          }
        }
      }
    }
  }
  post{
    failure{
      script{
        if (env.GIT_BRANCH == 'origin/master')
        emailext recipientProviders: [buildUser(), developers()], to: '$RS_RECIPIENTS, $DEFAULT_RECIPIENTS', subject: '$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!', body: '''$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS:
Check console output at $BUILD_URL to view the results.'''
      }
    }
  }
}
def isImportantChange() {
  def paths = ['docker/', 'docs/', 'pom.xml', 'src/main/']
  return currentBuild.changeSets.any { cs ->
    cs.items.any { item ->
      item.affectedPaths.any { path ->
        paths.any { imp -> path.startsWith(imp) || path == imp }
      }
    }
  }
}
