def checkout(String azureRepoUrl) {
    stage('Checkout') {
        steps {
            git url: azureRepoUrl
        }
    }
}

def build() {
    stage('Build') {
        steps {
            sh 'mvn clean package'
        }
    }
}

def test() {
    stage('Test') {
        steps {
            sh 'mvn test'
        }
    }
}

def buildDockerImage(String dockerImageName, String dockerfilePath) {
    stage('Build Docker Image') {
        steps {
            script {
                docker.build(dockerImageName, "-f ${dockerfilePath} .")
            }
        }
    }
}

def pushDockerImage(String dockerImageName, String dockerRegistryUrl, String dockerUsername, String dockerPassword) {
    stage('Push Docker Image') {
        steps {
            script {
                docker.withRegistry(dockerRegistryUrl, dockerUsername, dockerPassword) {
                    dockerImageName.push()
                }
            }
        }
    }
}

def deployToECS(String roleArn, String sessionName, String awsRegion, String ecsCluster, String ecsService, String dockerImageName) {
    stage('Deploy to ECS') {
        steps {
            script {
                def sts = awsSecurityToken(roleArn, sessionName)
                def credentials = sts.credentials

                withCredentials([[
                    $class: 'AmazonWebServicesCredentialsBinding',
                    accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                    secretKeyVariable: 'AWS_SECRET_ACCESS_KEY',
                    credentialsId: credentials.id
                ]]) {
                    sh """
                    aws configure set region ${awsRegion}
                    aws ecs update-service --cluster ${ecsCluster} --service ${ecsService} --force-new-deployment
                    """
                }
            }
        }
    }
}
