package com.htogo.app.ui.components

// HTML de los mapas (Leaflet), común a Android (WebView) e iOS (WKWebView). La página avisa a la
// app con enviarNativo('listo') al terminar de cargar y, en el selector, con
// enviarNativo('ubicacion:lat,lon') al mover el pin.

internal fun htmlMapaSelector(
    css: String, js: String, latitude: Double, longitude: Double, zoom: Int,
    isInteractive: Boolean, isDraggablePin: Boolean, heightDp: Int
): String = """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <style>
            $css

            html, body {
              width: 100%;
              height: 100%;
              margin: 0;
              padding: 0;
              background-color: #f1f5f9;
            }
            #map {
              width: 100%;
              height: ${heightDp}px;
              min-height: ${heightDp}px;
              background-color: #f1f5f9;
            }
            .custom-pin {
              display: flex;
              align-items: center;
              justify-content: center;
            }
            .leaflet-control-attribution {
              display: none !important;
            }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            $js
          </script>
          <script>
            // Mensajes a la app (Android: HToGoNativo; iOS: webkit.messageHandlers.htogo).
            function enviarNativo(m) {
              try {
                if (window.HToGoNativo) window.HToGoNativo.postMessage(m);
                else if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.htogo)
                  window.webkit.messageHandlers.htogo.postMessage(m);
              } catch (e) {}
            }

            var map = null;
            var marker = null;
            var initialLat = $latitude;
            var initialLon = $longitude;
            var initialZoom = $zoom;
            var isInteractive = $isInteractive;
            var isDraggable = $isDraggablePin;
            var fallbackHeight = $heightDp;

            function fixMapHeight() {
              var mapDiv = document.getElementById('map');
              if (!mapDiv) return;
              var h = window.innerHeight;
              if (h && h > 50) {
                mapDiv.style.height = h + 'px';
              } else {
                mapDiv.style.height = fallbackHeight + 'px';
              }
            }

            var pinSvg = '<svg width="38" height="38" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
              '<path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" fill="#0284C7" stroke="#FFFFFF" stroke-width="1.8"/>' +
              '<circle cx="12" cy="9" r="3.2" fill="#FFFFFF"/>' +
            '</svg>';

            var customIcon = L.divIcon({
              className: 'custom-pin',
              html: pinSvg,
              iconSize: [38, 38],
              iconAnchor: [19, 38]
            });

            function initMap() {
              try {
                if (map) return;
                fixMapHeight();
                var mapDiv = document.getElementById('map');
                if (!mapDiv) return;

                map = L.map('map', {
                  center: [initialLat, initialLon],
                  zoom: initialZoom,
                  zoomControl: isInteractive,
                  dragging: isInteractive,
                  touchZoom: isInteractive,
                  doubleClickZoom: isInteractive,
                  scrollWheelZoom: isInteractive,
                  attributionControl: false
                });

                // Capa de Google Maps (alternativa)
                var googleLayer = L.tileLayer('https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', { maxZoom: 20 });
                // Capa OpenStreetMap Humanitaria (HOT) con subdominios públicos libres
                var osmLayer = L.tileLayer('https://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png', {
                  subdomains: 'abc',
                  maxZoom: 19
                });
                var currentTileLayer = osmLayer; // OSM por defecto; Google como alternativa
                currentTileLayer.addTo(map);

                window.setCapa = function(tipo) {
                  if (currentTileLayer) map.removeLayer(currentTileLayer);
                  currentTileLayer = (tipo === 'osm') ? osmLayer : googleLayer;
                  currentTileLayer.addTo(map);
                };

                marker = L.marker([initialLat, initialLon], {
                  icon: customIcon,
                  draggable: isDraggable
                }).addTo(map);

                if (isDraggable) {
                  marker.on('dragend', function() {
                    var pos = marker.getLatLng();
                    enviarNativo('ubicacion:' + pos.lat + ',' + pos.lng);
                  });
                }

                if (isInteractive && isDraggable) {
                  map.on('click', function(e) {
                    marker.setLatLng(e.latlng);
                    enviarNativo('ubicacion:' + e.latlng.lat + ',' + e.latlng.lng);
                  });
                }

                if (window.ResizeObserver) {
                  var ro = new ResizeObserver(function() {
                    fixMapHeight();
                    if (map) {
                      map.invalidateSize(true);
                      console.log("ResizeObserver: map size is " + map.getSize().x + "x" + map.getSize().y);
                    }
                  });
                  ro.observe(mapDiv);
                }

                setTimeout(function() { fixMapHeight(); if (map) map.invalidateSize(true); }, 100);
                setTimeout(function() { fixMapHeight(); if (map) map.invalidateSize(true); }, 350);
                setTimeout(function() { fixMapHeight(); if (map) map.invalidateSize(true); }, 800);
                console.log("OsmMapView Leaflet initialized at [" + initialLat + ", " + initialLon + "] size=" + map.getSize().x + "x" + map.getSize().y);
              } catch(e) {
                console.error("Error initMap: " + e.message);
              }
            }

            function updatePosition(lat, lon) {
              try {
                fixMapHeight();
                if (!map) {
                  initMap();
                }
                if (map && marker) {
                  map.setView([lat, lon], map.getZoom() || initialZoom);
                  marker.setLatLng([lat, lon]);
                  setTimeout(function() { fixMapHeight(); if (map) map.invalidateSize(true); }, 150);
                  console.log("OsmMapView position updated to [" + lat + ", " + lon + "] size=" + map.getSize().x + "x" + map.getSize().y);
                }
              } catch(e) {
                console.error("Error updatePosition: " + e.message);
              }
            }

            if (document.readyState === 'complete' || document.readyState === 'interactive') {
              initMap();
            } else {
              document.addEventListener('DOMContentLoaded', initMap);
            }
            enviarNativo('listo');
          </script>
        </body>
        </html>
""".trimIndent()

internal fun htmlMapaRuta(
    css: String, js: String, destLat: Double, destLon: Double,
    isInteractive: Boolean, seguirRepartidor: Boolean
): String = """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <style>
            $css
            html, body { width: 100%; height: 100%; margin: 0; padding: 0; background: #DDE7EE; overflow: hidden; }
            #wrap { position: absolute; inset: 0; overflow: hidden; }
            #map { position: absolute; left: 0; top: 0; width: 100%; height: 100%; background: #DDE7EE;
                   transform-origin: 50% 50%; transition: transform .6s ease-out; }
            .pin { display: flex; align-items: center; justify-content: center; }
            .rep { position: relative; width: 48px; height: 48px; }
            .rep .rumbo { position: absolute; inset: 0; transition: transform .4s linear; }
            .ctl { position: absolute; right: 12px; width: 44px; height: 44px; border-radius: 22px; background: #fff;
                   box-shadow: 0 2px 8px rgba(15,23,42,.25); display: flex; align-items: center; justify-content: center;
                   z-index: 1000; -webkit-tap-highlight-color: transparent; }
            #brujula { top: calc(var(--arriba, 96px) + 54px); }
            #brujula svg { transition: transform .6s ease-out; }
            #estado { position: absolute; left: 12px; top: var(--arriba, 96px); z-index: 1000; display: none; align-items: center; gap: 6px;
                       background: #fff; border-radius: 99px; padding: 6px 12px; box-shadow: 0 2px 8px rgba(15,23,42,.2);
                       font: 600 12px sans-serif; color: #0F172A; }
            #estado i { width: 10px; height: 10px; border-radius: 5px; background: var(--estado, #0284C7); display: inline-block; }
            #centrar { top: calc(var(--arriba, 96px) + 106px); display: none; color: #0284C7; font: 600 12px sans-serif; width: auto; padding: 0 14px; }
            .leaflet-control-attribution { display: none !important; }
            .parada { width: 28px; height: 28px; border-radius: 14px; background: #0F172A; color: #fff;
                      border: 2.5px solid #fff; box-shadow: 0 2px 6px rgba(15,23,42,.45);
                      font: 800 12px sans-serif; display: flex; align-items: center; justify-content: center; }
            .parada.siguiente { width: 32px; height: 32px; border-radius: 16px; font-size: 13px; }
            .base-pin { width: 32px; height: 32px; border-radius: 10px; background: #0F172A; color: #fff;
                        border: 2.5px solid #38BDF8; box-shadow: 0 2px 6px rgba(15,23,42,.45);
                        font: 700 15px sans-serif; display: flex; align-items: center; justify-content: center; }
          </style>
        </head>
        <body>
          <div id="wrap"><div id="map"></div></div>
          <div id="brujula" class="ctl" style="display:none" title="Norte / rumbo">
            <svg width="28" height="28" viewBox="0 0 28 28"><text x="14" y="7.5" text-anchor="middle" font-size="7.5" font-weight="700" font-family="sans-serif" fill="#EF4444">N</text>
              <path d="M14 9 L18 15.5 L14 14 L10 15.5 Z" fill="#EF4444"/><path d="M14 25 L10 15.5 L14 17 L18 15.5 Z" fill="#94A3B8"/></svg>
          </div>
          <div id="centrar" class="ctl">◎ Centrar</div>
          <div id="estado"><i></i><span></span></div>
          <script>$js</script>
          <script>
            // Mensajes a la app (Android: HToGoNativo; iOS: webkit.messageHandlers.htogo).
            function enviarNativo(m) {
              try {
                if (window.HToGoNativo) window.HToGoNativo.postMessage(m);
                else if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.htogo)
                  window.webkit.messageHandlers.htogo.postMessage(m);
              } catch (e) {}
            }

            // Marcador del repartidor: círculo con gota y una flecha exterior que apunta a su rumbo.
            var repIcon = L.divIcon({ className: 'pin', iconSize: [48, 48], iconAnchor: [24, 24], html:
              '<div class="rep"><svg class="rumbo" width="48" height="48" viewBox="0 0 48 48">' +
              '<path d="M24 0 L30 9 L18 9 Z" style="fill:var(--estado,#0284C7)" stroke="#FFFFFF" stroke-width="1.5"/></svg>' +
              '<svg width="48" height="48" viewBox="0 0 48 48" style="position:absolute;inset:0">' +
              '<circle cx="24" cy="24" r="16" style="fill:var(--estado,#0284C7)" stroke="#FFFFFF" stroke-width="3"/>' +
              '<path d="M24 15C20.6 20 18 23.4 18 26.6A6 6 0 0 0 30 26.6C30 23.4 27.4 20 24 15Z" fill="#FFFFFF"/></svg></div>' });
            var destIcon = L.divIcon({ className: 'pin', iconSize: [42, 48], iconAnchor: [21, 46], html:
              '<svg width="42" height="48" viewBox="0 0 42 48" xmlns="http://www.w3.org/2000/svg">' +
              '<path d="M21 2C11.61 2 4 9.61 4 19C4 31.75 21 46 21 46S38 31.75 38 19C38 9.61 30.39 2 21 2Z" style="fill:var(--destino,#10B981)" stroke="#FFFFFF" stroke-width="2.5"/>' +
              '<circle cx="21" cy="19" r="6.5" fill="#FFFFFF"/></svg>' });

            // Dentro de Compose el WebView arranca con altura 0 y "height: 100%" deja el mapa en
            // 0 px: se fija la altura real en píxeles y Leaflet recalcula su tamaño (igual que OsmMapView).
            // Cuando SEGUIR está activo el contenedor mantiene siempre el tamaño de la diagonal (lado × lado)
            // para que al rotar o arrastrar nunca cambie de dimensiones ni salte el origen de píxeles.
            var SEGUIR = $seguirRepartidor;
            function ajustarTamano() {
              var div = document.getElementById('map');
              var w = window.innerWidth || 360, h = window.innerHeight;
              h = (h && h > 50 ? h : 400);
              var lado = SEGUIR ? Math.ceil(Math.sqrt(w * w + h * h)) : 0;
              div.style.width = (SEGUIR ? lado : w) + 'px';
              div.style.height = (SEGUIR ? lado : h) + 'px';
              div.style.left = (SEGUIR ? (w - lado) / 2 : 0) + 'px';
              div.style.top = (SEGUIR ? (h - lado) / 2 : 0) + 'px';
              if (typeof map !== 'undefined' && map) {
                map.invalidateSize({ pan: false });
                if (SEGUIR && siguiendo && repPos) map.setView(centroVisible(repPos, map.getZoom()), map.getZoom(), { animate: false });
              }
            }
            ajustarTamano();

            var map = L.map('map', {
              center: [$destLat, $destLon], zoom: 15, zoomControl: false, attributionControl: false,
              dragging: $isInteractive, touchZoom: $isInteractive, doubleClickZoom: $isInteractive, scrollWheelZoom: $isInteractive,
              inertia: false
            });
            // Capa de Google Maps (alternativa)
            var googleLayer = L.tileLayer('https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', { maxZoom: 20 });
            // Capa OpenStreetMap Humanitaria (HOT) con subdominios públicos libres
            var osmLayer = L.tileLayer('https://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png', {
              subdomains: 'abc',
              maxZoom: 19
            });
            var currentLayer = osmLayer; // OSM por defecto; Google como alternativa
            currentLayer.addTo(map);

            function setCapa(tipo) {
              if (currentLayer) map.removeLayer(currentLayer);
              currentLayer = (tipo === 'osm') ? osmLayer : googleLayer;
              currentLayer.addTo(map);
            }

            if (window.ResizeObserver) new ResizeObserver(ajustarTamano).observe(document.body);
            window.addEventListener('resize', ajustarTamano);
            [100, 350, 800].forEach(function (ms) { setTimeout(ajustarTamano, ms); });

            var markerDest = L.marker([$destLat, $destLon], { icon: destIcon }).addTo(map).bindPopup('<b>Destino de entrega</b>');
            var markerRep = null, markerBase = null, lineasRuta = [], encuadrado = false, rutaPrevia = '';
            var rutaActual = [], paradasActuales = [], destino = [$destLat, $destLon];
            var PALETA_TRAMOS = ['#10B981', '#7C3AED', '#EA580C', '#2563EB', '#DB2777', '#0891B2'];
            var repPos = null, rumbo = 0, anim = null;
            var siguiendo = SEGUIR, rumboArriba = false, anguloMapa = 0;
            var brujula = document.getElementById('brujula'), centrar = document.getElementById('centrar');
            if (SEGUIR) brujula.style.display = 'flex';

            // Compensa la rotación CSS del contenedor #map al arrastrar para que el mapa se mueva
            // exactamente en la dirección del dedo del usuario aunque el Norte esté desactivado.
            if (map.dragging && map.dragging._draggable) {
              var draggable = map.dragging._draggable;
              var origUpdatePosition = draggable._updatePosition;
              draggable._updatePosition = function () {
                if ( this._startPos && this._newPos && Math.abs(anguloMapa % 360) > 0.5 ) {
                  var dx = this._newPos.x - this._startPos.x;
                  var dy = this._newPos.y - this._startPos.y;
                  var rad = anguloMapa * Math.PI / 180;
                  var rx = dx * Math.cos(rad) - dy * Math.sin(rad);
                  var ry = dx * Math.sin(rad) + dy * Math.cos(rad);
                  this._newPos = this._startPos.add(L.point(rx, ry));
                }
                origUpdatePosition.call(this);
              };
            }

            function distM(a, b) {
              var R = 6371000, r = Math.PI / 180;
              var dLat = (b[0] - a[0]) * r, dLon = (b[1] - a[1]) * r;
              var x = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(a[0] * r) * Math.cos(b[0] * r) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
              return 2 * R * Math.asin(Math.sqrt(x));
            }
            function rumboEntre(a, b) {
              var r = Math.PI / 180, y = Math.sin((b[1] - a[1]) * r) * Math.cos(b[0] * r);
              var x = Math.cos(a[0] * r) * Math.sin(b[0] * r) - Math.sin(a[0] * r) * Math.cos(b[0] * r) * Math.cos((b[1] - a[1]) * r);
              return (Math.atan2(y, x) / r + 360) % 360;
            }

            function limpiarLineas() {
              for (var i = 0; i < lineasRuta.length; i++) {
                map.removeLayer(lineasRuta[i]);
              }
              lineasRuta = [];
            }

            function agregarSegmentoColor(pts, colorHex, esReal) {
              if (!pts || pts.length < 2) return;
              if (esReal) {
                var borde = L.polyline(pts, {
                  color: '#FFFFFF', weight: 10, opacity: 0.92, lineJoin: 'round', lineCap: 'round'
                }).addTo(map);
                var principal = L.polyline(pts, {
                  color: colorHex, weight: 6, opacity: 0.96, lineJoin: 'round', lineCap: 'round'
                }).addTo(map);
                var guia = L.polyline(pts, {
                  color: '#FFFFFF', weight: 1.8, opacity: 0.6, dashArray: '6, 12', lineJoin: 'round', lineCap: 'round'
                }).addTo(map);
                lineasRuta.push(borde, principal, guia);
              } else {
                var punteada = L.polyline(pts, {
                  color: colorHex, weight: 4.5, opacity: 0.85, dashArray: '8, 8', lineJoin: 'round'
                }).addTo(map);
                lineasRuta.push(punteada);
              }
            }

            // Divide la polilínea completa en tramos por parada [Origen->P1, P1->P2, P2->P3...]
            function dividirPorParadas(ruta, paradas) {
              var tramos = [];
              var inicioIdx = 0;
              for (var k = 0; k < paradas.length; k++) {
                var meta = [paradas[k][0], paradas[k][1]];
                if (k === paradas.length - 1) {
                  var tramoFinal = ruta.slice(inicioIdx);
                  if (tramoFinal.length >= 2) tramos.push({ color: PALETA_TRAMOS[k % PALETA_TRAMOS.length], puntos: tramoFinal });
                  break;
                }
                var dMinGlobal = Infinity, idxGlobal = inicioIdx;
                for (var i = inicioIdx + 1; i < ruta.length; i++) {
                  var d = distM(ruta[i], meta);
                  if (d < dMinGlobal) { dMinGlobal = d; idxGlobal = i; }
                }
                var umbral = Math.max(95, dMinGlobal + 20);
                var idxCorte = idxGlobal;
                for (var j = inicioIdx + 1; j < ruta.length - 1; j++) {
                  var dj = distM(ruta[j], meta);
                  var djNext = distM(ruta[j + 1], meta);
                  if (dj <= umbral && dj <= djNext) {
                    idxCorte = j;
                    break;
                  }
                }
                var sub = ruta.slice(inicioIdx, idxCorte + 1);
                if (sub.length >= 2) {
                  tramos.push({ color: PALETA_TRAMOS[k % PALETA_TRAMOS.length], puntos: sub });
                }
                inicioIdx = idxCorte;
              }
              return tramos;
            }

            // Proyecta un punto [lat, lon] sobre el segmento más cercano de una polilínea vial.
            function proyectarEnRuta(pos, pts) {
              if (!pts || pts.length < 2) return null;
              var cosLat = Math.max(0.1, Math.cos(pos[0] * Math.PI / 180));
              var mejorIdx = 0, mejorQ = pts[0], minDist = Infinity;
              for (var i = 0; i < pts.length - 1; i++) {
                var a = pts[i], b = pts[i + 1];
                var ax = a[1] * cosLat, ay = a[0];
                var bx = b[1] * cosLat, by = b[0];
                var px = pos[1] * cosLat, py = pos[0];
                var dx = bx - ax, dy = by - ay;
                var lenSq = dx * dx + dy * dy;
                var t = lenSq > 1e-12 ? Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / lenSq)) : 0;
                var q = [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t];
                var d = distM(pos, q);
                if (d < minDist) { minDist = d; mejorIdx = i; mejorQ = q; }
              }
              return { segIdx: mejorIdx, punto: mejorQ, distM: minDist };
            }

            // Respaldo tipo cuadrícula vial cuando aún se está recalculando A*, evitando líneas rectas diagonales.
            function rutaUrbanaRespaldo(desde, hasta) {
              var d = distM(desde, hasta);
              if (d <= 60) return [desde, hasta];
              var dLat = hasta[0] - desde[0], dLon = hasta[1] - desde[1];
              var perpLat = Math.abs(dLat) < 0.0008 ? 0.00065 : 0;
              var perpLon = Math.abs(dLon) < 0.0008 ? 0.00065 : 0;
              return [
                desde,
                [desde[0] + dLat * 0.35 + perpLat, desde[1] + dLon * 0.04],
                [desde[0] + dLat * 0.38 + perpLat, desde[1] + dLon * 0.55 + perpLon],
                [desde[0] + dLat * 0.78, desde[1] + dLon * 0.58 + perpLon],
                [desde[0] + dLat * 0.82, desde[1] + dLon * 0.94],
                hasta
              ];
            }

            // Línea: en ruta multi-parada (OR-Tools) pinta cada tramo de distinto color; en ruta individual usa color de alto contraste.
            function dibujarLinea() {
              limpiarLineas();
              if (paradasActuales.length > 0 && rutaActual.length >= 2) {
                var tramos = dividirPorParadas(rutaActual, paradasActuales);
                if (repPos && tramos.length > 0) {
                  var mejorT = 0, mejorProy = null, dMin = Infinity;
                  for (var t = 0; t < tramos.length; t++) {
                    var pr = proyectarEnRuta(repPos, tramos[t].puntos);
                    if (pr && pr.distM < dMin) { dMin = pr.distM; mejorT = t; mejorProy = pr; }
                  }
                  if (mejorProy && mejorProy.distM <= 240) {
                    for (var t2 = mejorT; t2 < tramos.length; t2++) {
                      var seg;
                      if (t2 === mejorT) {
                        var colaT = tramos[t2].puntos.slice(mejorProy.segIdx + 1);
                        seg = (distM(repPos, mejorProy.punto) > 8 ? [repPos, mejorProy.punto] : [repPos]).concat(colaT);
                      } else {
                        seg = tramos[t2].puntos;
                      }
                      agregarSegmentoColor(seg, tramos[t2].color, true);
                    }
                  } else {
                    for (var tSin = 0; tSin < tramos.length; tSin++) {
                      agregarSegmentoColor(tramos[tSin].puntos, tramos[tSin].color, true);
                    }
                  }
                } else {
                  for (var t3 = 0; t3 < tramos.length; t3++) {
                    agregarSegmentoColor(tramos[t3].puntos, tramos[t3].color, true);
                  }
                }
                return;
              }

              var puntos = [];
              var esReal = false;
              if (rutaActual.length >= 2) {
                if (repPos) {
                  var proy = proyectarEnRuta(repPos, rutaActual);
                  if (proy && proy.distM <= 240) {
                    var resto = rutaActual.slice(proy.segIdx + 1);
                    puntos = (distM(repPos, proy.punto) > 8 ? [repPos, proy.punto] : [repPos]).concat(resto);
                    if (puntos.length < 2) {
                      puntos = distM(repPos, destino) <= 85 ? [repPos, destino] : rutaUrbanaRespaldo(repPos, destino);
                    }
                    esReal = true;
                  } else {
                    puntos = rutaUrbanaRespaldo(repPos, destino);
                    esReal = false;
                  }
                } else {
                  puntos = rutaActual;
                  esReal = true;
                }
              } else if (repPos) {
                puntos = rutaUrbanaRespaldo(repPos, destino);
                esReal = false;
              }
              if (puntos.length < 2) return;
              agregarSegmentoColor(puntos, '#7C3AED', esReal);
            }

            // Gira el mapa (rumbo arriba) solo mientras sigue al repartidor; si no, norte arriba.
            function aplicarRotacion() {
              var objetivo = (SEGUIR && rumboArriba) ? rumbo : 0;
              var delta = ((objetivo - anguloMapa) % 360 + 540) % 360 - 180; // giro más corto
              anguloMapa += delta;
              if (repPos && SEGUIR && siguiendo) map.setView(centroVisible(repPos, map.getZoom()), map.getZoom(), { animate: false });
              document.getElementById('map').style.transform = 'rotate(' + (-anguloMapa) + 'deg)';
              brujula.querySelector('svg').style.transform = 'rotate(' + (-anguloMapa) + 'deg)';
              brujula.style.boxShadow = rumboArriba
                ? '0 0 0 2.5px #0284C7, 0 2px 8px rgba(15,23,42,.25)'
                : '0 2px 8px rgba(15,23,42,.25)';
            }
            function ponerRumboFlecha() {
              var el = markerRep && markerRep.getElement();
              var f = el && el.querySelector('.rumbo');
              if (f) f.style.transform = 'rotate(' + rumbo + 'deg)';
            }
            // Respeta el zoom que elija el usuario; el acercamiento a 17 es solo al empezar o al centrar.
            var zoomFijado = false;
            // Alto (px) del panel de la app que tapa la parte de abajo del mapa: el repartidor se
            // centra en la parte visible, no en el centro de la pantalla.
            var inferior = 0;
            function centroVisible(p, z) {
              if (!inferior) return p;
              var a = anguloMapa * Math.PI / 180, d = inferior / 2;
              var pt = map.project(p, z).add([-d * Math.sin(a), d * Math.cos(a)]);
              return map.unproject(pt, z);
            }
            function seguirA(p) {
              if (!(SEGUIR && siguiendo)) return;
              var z = zoomFijado ? map.getZoom() : 17;
              map.setView(centroVisible(p, z), z, { animate: false });
              zoomFijado = true;
            }
            function setMargenInferior(px) {
              inferior = px;
              if (repPos && SEGUIR && siguiendo) seguirA(repPos);
            }

            // Desliza el marcador hasta la nueva posición (~1 s) con la cámara detrás.
            function moverA(destinoRep) {
              if (!repPos || distM(repPos, destinoRep) > 2000) {
                repPos = destinoRep; markerRep.setLatLng(repPos); dibujarLinea(); seguirA(repPos); return;
              }
              if (distM(repPos, destinoRep) > 4) {
                rumbo = rumboEntre(repPos, destinoRep); ponerRumboFlecha(); aplicarRotacion();
              }
              if (anim) cancelAnimationFrame(anim);
              var desde = repPos.slice(), t0 = performance.now(), dur = 1000;
              function paso(t) {
                var k = Math.min(1, (t - t0) / dur);
                repPos = [desde[0] + (destinoRep[0] - desde[0]) * k, desde[1] + (destinoRep[1] - desde[1]) * k];
                markerRep.setLatLng(repPos);
                dibujarLinea();
                seguirA(repPos);
                anim = k < 1 ? requestAnimationFrame(paso) : null;
              }
              anim = requestAnimationFrame(paso);
            }

            // El usuario explora el mapa: se deja de seguir sin rotar ni redimensionar a mitad del arrastre.
            map.on('dragstart', function () {
              if (!SEGUIR) return;
              if (anim) { cancelAnimationFrame(anim); anim = null; }
              if (!siguiendo) return;
              siguiendo = false;
              centrar.style.display = 'flex';
            });
            centrar.addEventListener('click', function () {
              siguiendo = true;
              centrar.style.display = 'none';
              if (repPos) {
                var z = Math.max(map.getZoom(), 16);
                map.setView(centroVisible(repPos, z), z, { animate: false });
              }
              aplicarRotacion();
            });
            brujula.addEventListener('click', function () {
              rumboArriba = !rumboArriba;
              if (!siguiendo) {
                siguiendo = true;
                centrar.style.display = 'none';
              }
              if (repPos) {
                var z = Math.max(map.getZoom(), 16);
                map.setView(centroVisible(repPos, z), z, { animate: false });
              }
              aplicarRotacion();
            });

            // RF-007: color y etiqueta del estado del pedido en los marcadores (destino gris si ya cerró).
            function setEstado(color, texto, cerrado) {
              document.documentElement.style.setProperty('--estado', color);
              document.documentElement.style.setProperty('--destino', cerrado ? color : '#10B981');
              var chip = document.getElementById('estado');
              chip.querySelector('span').textContent = texto;
              chip.style.display = texto ? 'flex' : 'none';
              if (cerrado) {
                if (anim) { cancelAnimationFrame(anim); anim = null; }
                if (markerRep) { map.removeLayer(markerRep); markerRep = null; }
                if (markerBase) { map.removeLayer(markerBase); markerBase = null; }
                repPos = null;
                rutaActual = [];
                limpiarLineas();
                map.setView(centroVisible(destino, 16), 16, { animate: true });
              } else {
                dibujarLinea();
              }
            }

            function setBase(lat, lon, nombre) {
              if (markerBase) { map.removeLayer(markerBase); markerBase = null; }
              if (lat === null || lon === null) return;
              var baseIcon = L.divIcon({
                className: 'pin', iconSize: [32, 32], iconAnchor: [16, 16],
                html: '<div class="base-pin" title="Purificadora">🏪</div>'
              });
              markerBase = L.marker([lat, lon], { icon: baseIcon, zIndexOffset: 450 })
                .addTo(map)
                .bindPopup('<b>🏪 ' + (nombre || 'Purificadora') + ' (Punto de partida)</b>');
            }

            // Ruta del día: marcadores numerados en el orden óptimo. lista = [[lat, lon, número], ...];
            // cada parada se colorea igual que su tramo de ruta correspondiente.
            var marcadoresParadas = [];
            function setParadas(lista) {
              marcadoresParadas.forEach(function (m) { map.removeLayer(m); });
              marcadoresParadas = [];
              paradasActuales = lista || [];
              if (!paradasActuales.length) {
                dibujarLinea();
                return;
              }
              map.removeLayer(markerDest);
              paradasActuales.forEach(function (p, i) {
                var colorParada = PALETA_TRAMOS[i % PALETA_TRAMOS.length];
                var icono = L.divIcon({ className: 'pin', iconSize: [32, 32], iconAnchor: [16, 16],
                  html: '<div class="parada' + (i === 0 ? ' siguiente' : '') + '" style="background:' + colorParada + '">' + p[2] + '</div>' });
                marcadoresParadas.push(
                  L.marker([p[0], p[1]], { icon: icono, zIndexOffset: 500 - i })
                    .addTo(map)
                    .bindPopup('<b>Parada ' + p[2] + ' · Tramo ' + p[2] + '</b>')
                );
              });
              dibujarLinea();
              var b = L.latLngBounds(paradasActuales.map(function (p) { return [p[0], p[1]]; }));
              if (repPos) b.extend(repPos);
              if (b.isValid()) map.fitBounds(b, { paddingTopLeft: [40, 120], paddingBottomRight: [40, 40 + inferior], maxZoom: 16 });
              encuadrado = true;
            }

            // Llamada desde Kotlin en cada cambio. ruta = [[lat,lon],...] o [] (línea recta punteada).
            function actualizar(repLat, repLon, destLat, destLon, ruta, etiqueta) {
              ajustarTamano();
              destino = [destLat, destLon];
              markerDest.setLatLng(destino);
              var firma = JSON.stringify(ruta.length >= 2 ? [ruta[0], ruta[ruta.length - 1], ruta.length] : []);
              rutaActual = ruta;
              if (repLat !== null) {
                if (!markerRep) {
                  markerRep = L.marker([repLat, repLon], { icon: repIcon, zIndexOffset: 1000 }).addTo(map);
                  repPos = null;
                }
                markerRep.bindPopup('<b>' + etiqueta + '</b>');
                moverA([repLat, repLon]);
              } else {
                if (anim) { cancelAnimationFrame(anim); anim = null; }
                if (markerRep) { map.removeLayer(markerRep); markerRep = null; }
                repPos = null;
                dibujarLinea();
              }
              // Sin seguimiento: encuadra repartidor + destino la primera vez o si cambió la ruta.
              if (!(SEGUIR && siguiendo) && repLat !== null && (!encuadrado || (ruta.length >= 2 && firma !== rutaPrevia))) {
                var b = rutaActual.length >= 2 ? L.latLngBounds(rutaActual) : L.latLngBounds([[repLat, repLon], destino]);
                b.extend([repLat, repLon]);
                if (b.isValid()) map.fitBounds(b, { paddingTopLeft: [50, 130], paddingBottomRight: [50, 50 + inferior], maxZoom: 16 });
                encuadrado = true;
              }
              if (ruta.length >= 2) rutaPrevia = firma;
            }
            enviarNativo('listo');
          </script>
        </body>
        </html>
""".trimIndent()
