package holdout.onlyexistsitemroot;

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
import holdout.onlyexistsitemroot.meta.SubMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * The M6 carrier&#39;s nested type (v3.2 seat 12, D52 H3): every attribute optional - an only-exists parent must have no required attribute (the oracle&#39;s law).
 * @version 0.0.0
 */
@RosettaDataType(value="Sub", builder=Sub.SubBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Sub", model="holdout", builder=Sub.SubBuilderImpl.class, version="0.0.0")
public interface Sub extends RosettaModelObject {

	SubMeta metaData = new SubMeta();

	/*********************** Getter Methods  ***********************/
	String getSname();
	List<String> getSubs();

	/*********************** Build Methods  ***********************/
	Sub build();
	
	Sub.SubBuilder toBuilder();
	
	static Sub.SubBuilder builder() {
		return new Sub.SubBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Sub> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Sub> getType() {
		return Sub.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("sname"), String.class, getSname(), this);
		processor.processBasic(path.newSubPath("subs"), String.class, getSubs(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface SubBuilder extends Sub, RosettaModelObjectBuilder {
		Sub.SubBuilder setSname(String sname);
		Sub.SubBuilder addSubs(String subs);
		Sub.SubBuilder addSubs(String subs, int idx);
		Sub.SubBuilder addSubs(List<String> subs);
		Sub.SubBuilder setSubs(List<String> subs);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("sname"), String.class, getSname(), this);
			processor.processBasic(path.newSubPath("subs"), String.class, getSubs(), this);
		}
		

		Sub.SubBuilder prune();
	}

	/*********************** Immutable Implementation of Sub  ***********************/
	class SubImpl implements Sub {
		private final String sname;
		private final List<String> subs;
		
		protected SubImpl(Sub.SubBuilder builder) {
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
		public List<String> getSubs() {
			return subs;
		}
		
		@Override
		public Sub build() {
			return this;
		}
		
		@Override
		public Sub.SubBuilder toBuilder() {
			Sub.SubBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Sub.SubBuilder builder) {
			ofNullable(getSname()).ifPresent(builder::setSname);
			ofNullable(getSubs()).ifPresent(builder::setSubs);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Sub _that = getType().cast(o);
		
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
			return "Sub {" +
				"sname=" + this.sname + ", " +
				"subs=" + this.subs +
			'}';
		}
	}

	/*********************** Builder Implementation of Sub  ***********************/
	class SubBuilderImpl implements Sub.SubBuilder {
	
		protected String sname;
		protected List<String> subs = new ArrayList<>();
		
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
		public List<String> getSubs() {
			return subs;
		}
		
		@RosettaAttribute("sname")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("sname")
		@Override
		public Sub.SubBuilder setSname(String _sname) {
			this.sname = _sname == null ? null : _sname;
			return this;
		}
		
		@RosettaAttribute("subs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("subs")
		@Override
		public Sub.SubBuilder addSubs(String _subs) {
			if (_subs != null) {
				this.subs.add(_subs);
			}
			return this;
		}
		
		@Override
		public Sub.SubBuilder addSubs(String _subs, int idx) {
			getIndex(this.subs, idx, () -> _subs);
			return this;
		}
		
		@Override
		public Sub.SubBuilder addSubs(List<String> subss) {
			if (subss != null) {
				for (final String toAdd : subss) {
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
		public Sub.SubBuilder setSubs(List<String> subss) {
			if (subss == null) {
				this.subs = new ArrayList<>();
			} else {
				this.subs = subss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Sub build() {
			return new Sub.SubImpl(this);
		}
		
		@Override
		public Sub.SubBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Sub.SubBuilder prune() {
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
		public Sub.SubBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Sub.SubBuilder o = (Sub.SubBuilder) other;
			
			
			merger.mergeBasic(getSname(), o.getSname(), this::setSname);
			merger.mergeBasic(getSubs(), o.getSubs(), (Consumer<String>) this::addSubs);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Sub _that = getType().cast(o);
		
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
			return "SubBuilder {" +
				"sname=" + this.sname + ", " +
				"subs=" + this.subs +
			'}';
		}
	}
}
