
TOP = Top
BUILD_DIR = build

PART = xc7a35tcpg236-1
CONFIG_PART = xc7a35t_0

SRCS = $(wildcard src/main/scala/*.scala) $(wildcard src/main/scala/**/*.scala)
BLACKBOXES =
BLACKBOXTARGETS = $(addprefix $(BUILD_DIR)/, $(BLACKBOXES))

VIVADO_ARGS = -nojournal -log $(BUILD_DIR)/vivado.log -tempDir $(BUILD_DIR)

TEST_FILE ?= test.txt
K ?= 4
W ?= 32
N ?= 1024
KS = 2 4 8 16 32 64 128
TESTS = $(wildcard test-files/*K-random.txt)
ENERGY_FILES = $(basename $(notdir $(TESTS)))

all: gen synth
gen: $(BUILD_DIR)/$(TOP).v
synth: $(BUILD_DIR)/$(TOP).bit


.Phony:
rtl-test:
	sbt test

.Phony:
test-files/%:
	@echo $@
	sbt "runMain util.TestGenerator	 -w $(W) -n $(N) -o $(notdir $@)"

.Phony:
performance-table:
	sbt "test:runMain Performance"


$(ENERGY_FILES):
	@for k in ${KS}; do \
  		echo $@_$${k}.rpt; \
  		$(MAKE) clean synth TEST_FILE=test-files/$(notdir $@).txt W=32 K=$${k}; \
  		cp $(BUILD_DIR)/power.rpt energy/$@_$${k}.rpt; \
	done

energy: $(ENERGY_FILES)

clean:
	rm -rf $(BUILD_DIR)

$(BUILD_DIR)/$(TOP).v: $(SRCS)
	@mkdir -p $(@D)
	sbt "runMain $(TOP) --target-dir $(BUILD_DIR) --test-file $(TEST_FILE) -k $(K) -w $(W)"

$(BUILD_DIR)/$(TOP).bit: $(BUILD_DIR)/$(TOP).v pinout.xdc $(BUILD_DIR)/synth.tcl
	vivado $(VIVADO_ARGS) -mode batch -source $(BUILD_DIR)/synth.tcl
	@rm -rf  usage_statistics_webtalk.html usage_statistics_webtalk.xml

download: $(BUILD_DIR)/$(TOP).bit $(BUILD_DIR)/config.tcl
	vivado $(VIVADO_ARGS) -mode batch -source $(BUILD_DIR)/config.tcl
	@rm -rf .Xil usage_statistics_webtalk.html usage_statistics_webtalk.xml webtalk.jou webtalk.log

$(BUILD_DIR)/synth.tcl: $(BLACKBOXTARGETS) $(BUILD_DIR)/$(TOP).v
	echo "\
read_verilog [ glob $(BUILD_DIR)/$(TOP).v $(BLACKBOXTARGETS) ]\n\
read_xdc ./pinout.xdc\n\
synth_design -top $(TOP) -part $(PART)\n\
opt_design\n\
place_design\n\
route_design\n\
report_timing_summary -file $(BUILD_DIR)/sta.rpt\n\
report_utilization -file $(BUILD_DIR)/util.rpt\n\
report_power -file build/power.rpt\n\
write_bitstream $(BUILD_DIR)/$(TOP).bit -force" > $(BUILD_DIR)/synth.tcl

$(BUILD_DIR)/config.tcl:
	echo "\
open_hw_manager \n\
connect_hw_server -allow_non_jtag \n\
open_hw_target \n\
current_hw_device [get_hw_devices $(CONFIG_PART)] \n\
refresh_hw_device -update_hw_probes false [lindex [get_hw_devices $(CONFIG_PART)] 0] \n\
set_property PROBES.FILE {} [get_hw_devices $(CONFIG_PART)] \n\
set_property FULL_PROBES.FILE {} [get_hw_devices $(CONFIG_PART)] \n\
set_property PROGRAM.FILE {./$(BUILD_DIR)/$(TOP).bit} [get_hw_devices $(CONFIG_PART)] \n\
program_hw_devices [get_hw_devices $(CONFIG_PART)] \n\
refresh_hw_device [lindex [get_hw_devices $(CONFIG_PART)] 0] \n\
close_hw_manager" > $(BUILD_DIR)/config.tcl
