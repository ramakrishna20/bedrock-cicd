def checkout(String gitlabRepoUrl) {
    stage('Checkout') {
        steps {
            git url: gitlabRepoUrl
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

def deployToECS(String awsAccessKey, String awsSecretKey, String awsRegion, String ecsCluster, String ecsService, String dockerImageName) {
    stage('Deploy to ECS') {
        steps {
            script {
                withCredentials([[
                    $class: 'AmazonWebServicesCredentialsBinding',
                    accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                    secretKeyVariable: 'AWS_SECRET_ACCESS_KEY',
                    credentialsId: 'aws-credentials-id'
                ]]) {
                    sh """
                    aws configure set aws_access_key_id ${awsAccessKey}
                    aws configure set aws_secret_access_key ${awsSecretKey}
                    aws configure set region ${awsRegion}
                    aws ecs update-service --cluster ${ecsCluster} --service ${ecsService} --force-new-deployment
                    """
                }
            }
        }
    }
}
