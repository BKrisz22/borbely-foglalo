package hu.szakdolgozat.borbely.barber.dto;

/**
 * A borbély-végpontok kétféle válasza: a tulajdonos a teljes adatokat kapja ({@link BarberResponse}),
 * mindenki más csak a nyilvános adatokat ({@link BarberPublicResponse}).
 */
public sealed interface BarberView permits BarberResponse, BarberPublicResponse {
}
