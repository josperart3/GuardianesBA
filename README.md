![GitHub top Language](https://img.shields.io/github/languages/top/tfg-projects-dit-us/GuardianesBA)
![GitHub forks](https://img.shields.io/github/forks/tfg-projects-dit-us/GuardianesBA?style=social)
![GitHub contributors](https://img.shields.io/github/contributors/tfg-projects-dit-us/GuardianesBA)
![GitHub Repo stars](https://img.shields.io/github/stars/tfg-projects-dit-us/GuardianesBA?style=social)
![GitHub repo size](https://img.shields.io/github/repo-size/tfg-projects-dit-us/GuardianesBA)
![GitHub watchers](https://img.shields.io/github/watchers/tfg-projects-dit-us/GuardianesBA)
![GitHub](https://img.shields.io/github/license/tfg-projects-dit-us/GuardianesBA)
# Proyecto GuardianesBA

Este proyecto es una aplicación de negocios jBPM con Spring Boot

Actualmente es una versión beta que gestiona únicamente el proceso de planificación (automática) de tareas asistenciales en un servicio clínico

Está desarrollado en el Departamento de Ingeniería Telemática de la Universidad de Sevilla

## Licencia

Este proyecto está licenciado bajo los términos de la [Licencia Pública General de GNU (GPL) versión 3](https://www.gnu.org/licenses/gpl-3.0.html).


## License

This project is licensed under the terms of the [GNU General Public License (GPL) version 3](https://www.gnu.org/licenses/gpl-3.0.html).

## Reconocimientos

Este proyecto es el resultado del trabajo desarrollado por los alumnos que a continuación se mencionan, bajo la supervisión de la profesora Isabel Román Martínez.

**Autores:**
- [Jose Carlos Rodríguez Morón](https://github.com/josrodmor11): TFG, versión actual del proyecto incluyendo el paradigma BPM para el proceso principal de generación de la planificación. Este repositorio recoge el trabajo realizado durante el desarrollo de esta tercera versión
- [Carmen Cohen Calvo](https://github.com/carcohcal): TFG Servicio de calendario para un servicio hospitalario, segunda versión del proyecto, que incluye la gestión de calendarios de los profesionales sanitarios
- [Miguel Ángel González-Alorda Cantero](https://github.com/miggoncan): TFG Servicio para la gestión de actividades asistenciales complementarias, primera versión del proyecto que no consideraba el paradigma BPM

**Supervisora:**
- [Isabel Román Martínez](https://github.com/Isabel-Roman), Profesora del Departamento de Ingeniería Telemática de la Universidad de Sevilla

La supervisión incluye la generación de ideas, la corrección, el desarrollo de algunos componentes y la orientación técnica durante todo el proceso de desarrollo.


## Módulos
1. kjar: base de conocimiento kie, procesos, reglas de negocio, etc...
2. service: la aplicación
3. model: POJOs compartidos entre kjar y service
## Dependencias
**En la versión actual utilizamos:**
* _spring boot starter_ [_2.6.15_](https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter/2.6.15)
* _kie server_: [_7.74.1.Final_](https://mvnrepository.com/artifact/org.kie/kie-server-spring-boot-starter/7.74.1.Final)

## Contributions

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement". Do not forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request
