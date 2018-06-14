## Requirements
### Machine that running ansible

* AWS credentials with permissions allowed to create security group and instances.
Setup it as described [here](https://docs.ansible.com/ansible/2.5/scenario_guides/guide_aws.html) 
or install and configure [AWS command line tools](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-welcome.html).
* Secret part of keypair to authenticate with new instance
* **docker-py** is required, to install run:
```pip install docker```

## Running
```ansible-play -i hosts --extra-vars "key_file=full-path-to-local-pem" deploy.yml```

Replace the *full-path-to-local-pem* with actual path to pem file that matches with 
AWS keypair.

## Running on Ubuntu 18.x

Unfortunately docker-ce has not released yet for bionic,
so it's possible to install from **edge** repositories only.

```
ansible-playbook -i hosts --extra-vars="docker_apt_release_channel=edge" deploy.yml
```

## Required variables
* **security_group** is the security group of deploying AWS instance
* **subnet_id** is the identity of the subnet
* **keypair** is the name of keypair to be assigned to newly deployed instance

## Additional variables

* **instance_type** is the AWS instance type, default is *t2.micro*,
* **image** is the AMI of AWS image, default value points to *CentOS 7* at *us-west-2* region
* **region** is the code of AWS region instance would be deployed to, default is the *us-west-2*
