# Java Payment Calculator

Microservice using old versions of Spring, Maven, and Java. Also uses a purposely vulnerable dependency.

## Build/Run Locally

The application uses a dependency from a private GitLab repository. To run the build locally, you must
authenticate to GitLab. Here are the steps:

1. Create a personal access token. See here for guidance: https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html
2. Add the following to your `settings.xml` (in your `.m2` directory):

   ```xml
   <settings>
     <servers>
       <server>
         <id>gitlab-maven</id>
         <configuration>
           <httpHeaders>
             <property>
               <name>Private-Token</name>
               <value>REPLACE_WITH_TOKEN</value>
             </property>
           </httpHeaders>
         </configuration>
       </server>
     </servers>
   </settings>
   ```
