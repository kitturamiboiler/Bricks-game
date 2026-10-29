package game.bricks.application.dto;

public record SessionView(String mode, String status, String address, int localPlayer,
                          NetworkMessage.Lobby lobby, GameSnapshot game) {}
