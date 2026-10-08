package chaos.s25.a4snap;

import chaos.s25.a4snap.meta.C25SubMeta;
import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Helper - relocated by the import axis; every attribute optional (an only-exists parent must have no required attribute - the oracle&#39;s law).
 * @version 1.0.0-SNAPSHOT
 */
@RosettaDataType(value="C25Sub", builder=C25Sub.C25SubBuilderImpl.class, version="1.0.0-SNAPSHOT")
@RuneDataType(value="C25Sub", model="chaos", builder=C25Sub.C25SubBuilderImpl.class, version="1.0.0-SNAPSHOT")
public interface C25Sub extends RosettaModelObject {

	C25SubMeta metaData = new C25SubMeta();

	/*********************** Getter Methods  ***********************/
	String getSname();
	List<BigDecimal> getSubs();

	/*********************** Build Methods  ***********************/
	C25Sub build();
	
	C25Sub.C25SubBuilder toBuilder();
	
	static C25Sub.C25SubBuilder builder() {
		return new C25Sub.C25SubBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C25Sub> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C25Sub> getType() {
		return C25Sub.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("sname"), String.class, getSname(), this);
		processor.processBasic(path.newSubPath("subs"), BigDecimal.class, getSubs(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C25SubBuilder extends C25Sub, RosettaModelObjectBuilder {
		C25Sub.C25SubBuilder setSname(String sname);
		C25Sub.C25SubBuilder addSubs(BigDecimal subs);
		C25Sub.C25SubBuilder addSubs(BigDecimal subs, int idx);
		C25Sub.C25SubBuilder addSubs(List<BigDecimal> subs);
		C25Sub.C25SubBuilder setSubs(List<BigDecimal> subs);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("sname"), String.class, getSname(), this);
			processor.processBasic(path.newSubPath("subs"), BigDecimal.class, getSubs(), this);
		}
		

		C25Sub.C25SubBuilder prune();
	}

	/*********************** Immutable Implementation of C25Sub  ***********************/
	class C25SubImpl implements C25Sub {
		private final String sname;
		private final List<BigDecimal> subs;
		
		protected C25SubImpl(C25Sub.C25SubBuilder builder) {
			this.sname = builder.getSname();
			this.subs = ofNullable(builder.getSubs()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("sname")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("sname")
		public String getSname() {
			return sname;
		}
		
		@Override
		@RosettaAttribute("subs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("subs")
		public List<BigDecimal> getSubs() {
			return subs;
		}
		
		@Override
		public C25Sub build() {
			return this;
		}
		
		@Override
		public C25Sub.C25SubBuilder toBuilder() {
			C25Sub.C25SubBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C25Sub.C25SubBuilder builder) {
			ofNullable(getSname()).ifPresent(builder::setSname);
			ofNullable(getSubs()).ifPresent(builder::setSubs);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C25Sub _that = getType().cast(o);
		
			if (!Objects.equals(sname, _that.getSname())) return false;
			if (!ListEquals.listEquals(subs, _that.getSubs())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (sname != null ? sname.hashCode() : 0);
			_result = 31 * _result + (subs != null ? subs.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C25Sub {" +
				"sname=" + this.sname + ", " +
				"subs=" + this.subs +
			'}';
		}
	}

	/*********************** Builder Implementation of C25Sub  ***********************/
	class C25SubBuilderImpl implements C25Sub.C25SubBuilder {
	
		protected String sname;
		protected List<BigDecimal> subs = new ArrayList<>();
		
		@Override
		@RosettaAttribute("sname")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("sname")
		public String getSname() {
			return sname;
		}
		
		@Override
		@RosettaAttribute("subs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("subs")
		public List<BigDecimal> getSubs() {
			return subs;
		}
		
		@RosettaAttribute("sname")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("sname")
		@Override
		public C25Sub.C25SubBuilder setSname(String _sname) {
			this.sname = _sname == null ? null : _sname;
			return this;
		}
		
		@RosettaAttribute("subs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("subs")
		@Override
		public C25Sub.C25SubBuilder addSubs(BigDecimal _subs) {
			if (_subs != null) {
				this.subs.add(_subs);
			}
			return this;
		}
		
		@Override
		public C25Sub.C25SubBuilder addSubs(BigDecimal _subs, int idx) {
			getIndex(this.subs, idx, () -> _subs);
			return this;
		}
		
		@Override
		public C25Sub.C25SubBuilder addSubs(List<BigDecimal> subss) {
			if (subss != null) {
				for (final BigDecimal toAdd : subss) {
					this.subs.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("subs")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("subs")
		@Override
		public C25Sub.C25SubBuilder setSubs(List<BigDecimal> subss) {
			if (subss == null) {
				this.subs = new ArrayList<>();
			} else {
				this.subs = subss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public C25Sub build() {
			return new C25Sub.C25SubImpl(this);
		}
		
		@Override
		public C25Sub.C25SubBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C25Sub.C25SubBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getSname()!=null) return true;
			if (getSubs()!=null && !getSubs().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C25Sub.C25SubBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C25Sub.C25SubBuilder o = (C25Sub.C25SubBuilder) other;
			
			
			merger.mergeBasic(getSname(), o.getSname(), this::setSname);
			merger.mergeBasic(getSubs(), o.getSubs(), (Consumer<BigDecimal>) this::addSubs);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C25Sub _that = getType().cast(o);
		
			if (!Objects.equals(sname, _that.getSname())) return false;
			if (!ListEquals.listEquals(subs, _that.getSubs())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (sname != null ? sname.hashCode() : 0);
			_result = 31 * _result + (subs != null ? subs.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C25SubBuilder {" +
				"sname=" + this.sname + ", " +
				"subs=" + this.subs +
			'}';
		}
	}
}
