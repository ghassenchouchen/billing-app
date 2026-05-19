import os

services = [
    'api-gateway', 'authentication-service', 'billing-service',
    'boutique-service', 'catalog-service', 'customer-service',
    'payment-service', 'subscription-service', 'usage-service'
]

base_dir = '/home/shadowfax/billing-app/spring-backend'

for svc in services:
    pom_path = os.path.join(base_dir, svc, 'pom.xml')
    if not os.path.exists(pom_path):
        continue
        
    with open(pom_path, 'r') as f:
        content = f.read()
        
    changed = False
    
    # 1. Add Eureka Client dependency
    if 'spring-cloud-starter-netflix-eureka-client' not in content:
        dep = """
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
"""
        content = content.replace('</dependencies>', dep + '    </dependencies>')
        changed = True
        
    # 2. Add DependencyManagement if missing
    if 'spring-cloud-dependencies' not in content and 'api-gateway' not in svc:
        dep_mgmt = """
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
"""
        # We need to make sure spring-cloud.version is defined
        if '<spring-cloud.version>' not in content:
            content = content.replace('</properties>', '    <spring-cloud.version>2023.0.0</spring-cloud.version>\n    </properties>')
        content = content.replace('    <build>', dep_mgmt + '\n    <build>')
        changed = True

    if changed:
        with open(pom_path, 'w') as f:
            f.write(content)
        print(f"Updated pom.xml for {svc}")
        
    # 3. Update application.yml
    yml_path = os.path.join(base_dir, svc, 'src/main/resources/application.yml')
    if os.path.exists(yml_path):
        with open(yml_path, 'r') as f:
            yml_content = f.read()
            
        if 'eureka:' not in yml_content:
            eureka_config = """
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URI:http://localhost:8761/eureka}
  instance:
    prefer-ip-address: true
"""
            with open(yml_path, 'a') as f:
                f.write(eureka_config)
            print(f"Updated application.yml for {svc}")

print("Done updating microservices.")
