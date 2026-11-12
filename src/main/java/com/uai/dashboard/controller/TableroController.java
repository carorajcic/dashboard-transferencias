package com.uai.dashboard.controller;

import com.uai.dashboard.dto.Dtos.TransferenciaDetalle;
import com.uai.dashboard.repository.TableroRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Drill-down: Nivel 1 (bancos) → Nivel 2 (transferencias del banco) → Nivel 3 (detalle). */
@Controller
public class TableroController {

    private static final Set<String> ESTADOS = Set.of("PENDIENTE", "APROBADA", "RECHAZADA", "REVERTIDA");

    private final TableroRepository repo;

    public TableroController(TableroRepository repo) {
        this.repo = repo;
    }

    /** Nivel 1 */
    @GetMapping("/tablero")
    public String nivel1(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                         Model model) {
        var bancos = repo.bancos(desde, hasta);
        model.addAttribute("bancos", bancos);
        model.addAttribute("totales", repo.totales(desde, hasta));
        model.addAttribute("nombresBancos", bancos.stream().map(b -> b.nombre()).toList());
        model.addAttribute("volumenBancos", bancos.stream().map(b -> b.total()).toList());
        agregarComunes(model, desde, hasta);
        return "tablero-nivel1";
    }

    /** Nivel 2 */
    @GetMapping("/tablero/banco/{id}")
    public String nivel2(@PathVariable int id,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                         @RequestParam(required = false) String estado,
                         @RequestParam(required = false) Integer pais,
                         Model model) {
        var banco = repo.banco(id, desde, hasta);
        if (banco.isEmpty()) {
            return "redirect:/tablero";
        }
        String estadoValido = (estado != null && ESTADOS.contains(estado)) ? estado : null;
        var evolucion = repo.evolucion(id, desde, hasta);

        model.addAttribute("banco", banco.get());
        model.addAttribute("productos", repo.productosHabilitados(id));
        model.addAttribute("transferencias", repo.transferencias(id, desde, hasta, estadoValido, pais));
        model.addAttribute("paises", repo.paises());
        model.addAttribute("estados", List.of("APROBADA", "RECHAZADA", "PENDIENTE", "REVERTIDA"));
        model.addAttribute("estadoSel", estadoValido);
        model.addAttribute("paisSel", pais);
        model.addAttribute("dias", evolucion.stream().map(e -> e.dia().toString()).toList());
        model.addAttribute("aprobadasDia", evolucion.stream().map(e -> e.aprobadas()).toList());
        model.addAttribute("rechazadasDia", evolucion.stream().map(e -> e.rechazadas()).toList());
        agregarComunes(model, desde, hasta);
        return "tablero-nivel2";
    }

    /** Nivel 3 */
    @GetMapping("/tablero/transferencia/{id}")
    public String nivel3(@PathVariable long id,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                         Model model) {
        var detalle = repo.detalle(id);
        if (detalle.isEmpty()) {
            return "redirect:/tablero";
        }
        TransferenciaDetalle d = detalle.get();
        model.addAttribute("t", d);
        agregarComunes(model, desde, hasta);
        return "tablero-nivel3";
    }

    private void agregarComunes(Model model, LocalDate desde, LocalDate hasta) {
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("umbralVerde", repo.getUmbralVerde());
        model.addAttribute("umbralAmarillo", repo.getUmbralAmarillo());
    }
}
