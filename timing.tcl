package require cmdline

set parameters {
    {t.arg "" "top level entity"}
    {v.arg "" "verilog files"}
    {p.arg "" "part number"}
    {f.arg "" "other files"}
    {x.arg "" "constraint file"}
}

set usage "- A simple script to demo cmdline parsing"

if {[catch {array set options [cmdline::getoptions ::argv $parameters $usage]}]} {
    puts [cmdline::usage $parameters $usage]
}


create_project -force $options(t) -part $options(p)

add_files $options(v) 
add_files $options(x)
add_files $options(f)

launch_runs synth_1
wait_on_run synth_1

open_run synth_1 -name netlist_1
set_delay_model -interconnect estimated
report_timing_summary -file sta_post_synth.rpt
report_utilization -file util_post_synth.rpt
report_power -file power_post_synth.rpt
start_gui