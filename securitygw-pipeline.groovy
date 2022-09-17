def runScript(command) {
    script {
        sh script: 'set +x ; $command 2>&1 && echo \"status:\$?\" || echo \"status:\$?\" ; exit 0', returnStdout: true
    }
}

pipeline {
    agent any

    stages {
        stage('Get configuration') {
            steps {
                git changelog: false, credentialsId: 'jenkins_git', poll: false, url: 'https://github.com/andreika47/SecurityGW.git'
            } 
        }

        stage('Check') {
            environment {
                PATH_TO_GIXY_RES = '/home/gixy-out.json'
            }
            steps {
                script {
                    GIXY_RES = runScript('gixy ./nginx/nginx.conf -f json -o $PATH_TO_GIXY_RES').trim()

                    WHOAMI_RES = runScript('whoami').trim()
                    WHOAMI_RES2 = sh(script: 'whoami', returnStatus: true).trim()
                    echo "whoami: ${WHOAMI_RES}"
                    echo "whoami2: ${WHOAMI_RES2}"
                    
                    def res = readJSON file: PATH_TO_GIXY_RES
                    int UNSPEC = 0 as Integer
                    int LOW = 0 as Integer
                    int MED = 0 as Integer
                    int HIGH = 0 as Integer

                    for(bug in res) {
                        switch(bug.severity) {
                            case 'UNSPECIFIED':
                            UNSPEC += 1
                            break

                            case 'LOW':
                            LOW += 1
                            break

                            case 'MEDIUM':
                            MED += 1
                            break

                            case 'HIGH':
                            HIGH += 1
                            break

                            default:
                            break
                        }
                    }

                    echo "UNSPECIFIED: ${UNSPEC}\nLOW: ${LOW}\nMEDIUM: ${MED}\nHIGH: ${HIGH}\nAdditional info: ${PATH_TO_GIXY_RES}"

                    if(UNSPEC > 0 || MED > 3 || HIGH > 0) {
                        echo "Too much bugs"
                        sh "exit 1"
                    }
                }
            }
        }

        stage('Configure') {
            steps {
                script {
                    sh 'cp -r nginx /etc'
                    sh 'rm -r nginx'
                    CONF_RES = runScript('nginx -s reload').trim()

                    echo CONF_RES
                }
            }
        }
    }
}