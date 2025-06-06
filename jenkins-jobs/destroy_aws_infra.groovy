pipeline {
    agent any

    environment {
        TF_DIR = 'terraform-infra'
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/vasichkin/kubernetes-terraform-ansible.git',
                    branch: 'master'
            }
        }

        stage('Prepare tfvars') {
          steps {
            dir('terraform-infra') {
              withCredentials([usernamePassword(credentialsId: 'aws_creds', usernameVariable: 'AWS_ACCESS_KEY', passwordVariable: 'AWS_SECRET_KEY')]) {
                script {
                  def exampleContent = readFile('tfvars.example')
                  def tfvarsContent = exampleContent
                    .replaceAll(/AWS_ACCESS_KEY/, env.AWS_ACCESS_KEY)
                    .replaceAll(/AWS_SECRET_KEY/, env.AWS_SECRET_KEY)
                    .replaceAll(/OWNER/, env.OWNER)

                  writeFile file: 'terraform.tfvars', text: tfvarsContent
                }
              }
            }
          }
        }

        stage('Provision Infrastructure') {
            steps {
                dir("${TF_DIR}") {
                    sh '''
                        echo "Initializing Terraform..."
                        terraform init

                        echo "Applying Terraform..."
                        terraform destroy -auto-approve -no-color
                    '''
                }
            }
        }
    }

    post {
        success {
            echo '✅ Infrastructure destroyed successfully! '
        }
        failure {
            echo '❌ Failed to destroy infrastructure.'
        }
    }
}
