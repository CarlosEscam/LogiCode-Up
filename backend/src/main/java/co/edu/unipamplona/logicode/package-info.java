/**
 * LogiCode: monolito modular.
 *
 * <p>Cada módulo funcional es un paquete hijo directo de este paquete y se divide en cuatro capas:
 * <ul>
 *   <li>{@code api}: controladores REST, DTO, validación de formato y traducción de errores HTTP.</li>
 *   <li>{@code application}: casos de uso, transacciones, autorización y puertos hacia infraestructura.</li>
 *   <li>{@code domain}: entidades, invariantes y políticas puras del negocio, sin Spring ni JPA de infraestructura.</li>
 *   <li>{@code infrastructure}: persistencia, archivos, correo, IA y tareas en segundo plano.</li>
 * </ul>
 * Las reglas de dependencia entre capas se verifican con pruebas de arquitectura (ArchUnit).
 * Ver {@code docs/arquitectura.md}.
 */
package co.edu.unipamplona.logicode;
